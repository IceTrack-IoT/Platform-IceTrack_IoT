package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.EdgeEventCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.EdgeEventOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordEdgeEventCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventResult;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.EdgeEventRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;

/**
 * Default implementation of {@link EdgeEventCommandService}.
 *
 * <p>Edge events are what open thermal excursion alerts: the gateway evaluates every reading
 * locally and only reports an excursion once it has lasted the policy's sustained duration, so the
 * cloud no longer opens alerts from single reading batches.</p>
 *
 * <p>Idempotency works like reading ingestion. An event whose {@code eventUid} is already recorded
 * is answered as a duplicate and nothing is processed. Two concurrent deliveries of the same event
 * can both pass that check; the unique index on {@code event_uid} rejects the second insert, its
 * transaction - alert change included - is rolled back, and it is answered as a duplicate too. That
 * is why the work runs in a {@link TransactionTemplate}: the violation must be caught after the
 * rollback, outside the transaction.</p>
 */
@Service
@Slf4j
public class EdgeEventCommandServiceImpl implements EdgeEventCommandService {

  private final EdgeEventRepository edgeEventRepository;
  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final SensorReadingRepository sensorReadingRepository;
  private final TransactionTemplate transactionTemplate;

  public EdgeEventCommandServiceImpl(
      EdgeEventRepository edgeEventRepository,
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      SensorReadingRepository sensorReadingRepository,
      PlatformTransactionManager transactionManager) {
    this.edgeEventRepository = edgeEventRepository;
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.sensorReadingRepository = sensorReadingRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  // inherited javadoc
  @Override
  public EdgeEventOutcome handle(RecordEdgeEventCommand command) {
    var existing = edgeEventRepository.findByEventUid(command.eventUid());
    if (existing.isPresent()) {
      log.info("Duplicate edge event {} from device {}", command.eventUid(), command.deviceId());
      return new EdgeEventOutcome.Duplicate(existing.get());
    }
    try {
      return transactionTemplate.execute(status -> new EdgeEventOutcome.Processed(process(command)));
    } catch (DataIntegrityViolationException e) {
      return edgeEventRepository.findByEventUid(command.eventUid())
          .<EdgeEventOutcome>map(stored -> {
            log.info("Concurrent duplicate edge event {} from device {}", command.eventUid(), command.deviceId());
            return new EdgeEventOutcome.Duplicate(stored);
          })
          .orElseThrow(() -> e);
    }
  }

  /**
   * Applies the event to the unit's active excursion alert and records it, in the caller's
   * transaction.
   *
   * @param command the event
   * @return the stored record of the event
   */
  private EdgeEvent process(RecordEdgeEventCommand command) {
    var activeAlert = alertRepository.findOpenAlertByEquipmentIdAndType(
        command.equipmentId(), AlertType.TEMPERATURE_EXCURSION);
    EdgeEventResult result;
    Long alertId;
    if (command.type() == EdgeEventType.THERMAL_EXCURSION_DETECTED) {
      if (activeAlert.isPresent()) {
        result = EdgeEventResult.ALERT_ALREADY_ACTIVE;
        alertId = activeAlert.get().getId();
      } else {
        var alert = openAlert(command);
        result = EdgeEventResult.ALERT_OPENED;
        alertId = alert.getId();
      }
    } else if (activeAlert.isPresent()) {
      var alert = activeAlert.get();
      alert.resolveFromEdge(command.peakTemperatureCelsius(), command.occurredAt());
      alertId = alertRepository.save(alert).getId();
      result = EdgeEventResult.ALERT_RESOLVED;
    } else {
      result = EdgeEventResult.NO_ACTIVE_ALERT;
      alertId = null;
    }
    var stored = edgeEventRepository.save(new EdgeEvent(
        null, command.eventUid(), command.deviceId(), command.equipmentId(), command.type(), command.occurredAt(),
        command.temperatureCelsius(), command.peakTemperatureCelsius(), result, alertId));
    log.info("Edge event {} ({}) from device {} on equipment {}: {} alert {}",
        stored.getEventUid(), stored.getType(), stored.getDeviceId(), stored.getEquipmentId(), result, alertId);
    return stored;
  }

  /**
   * Opens the CRITICAL alert of a detected excursion and announces it.
   *
   * @param command the detection event
   * @return the persisted alert
   */
  private Alert openAlert(RecordEdgeEventCommand command) {
    // The default is seeded at startup; the in-memory fallback only guards against a database in
    // which it was deleted.
    var policy = alertPolicyRepository.findApplicableTo(command.equipmentId())
        .orElseGet(AlertPolicy::platformDefault);
    // Only a reading of the same unit may be named as the trigger.
    var triggeringReadingId = command.readingUid() == null ? null
        : sensorReadingRepository.findByReadingUid(command.readingUid())
            .filter(reading -> command.equipmentId().equals(reading.getEquipmentId().equipmentId()))
            .map(SensorReading::getId)
            .orElse(null);
    var alert = Alert.raisedFromEdge(
        command.equipmentId(), triggeringReadingId, command.temperatureCelsius(), command.occurredAt(),
        Duration.ofMinutes(policy.getSustainedExcursionMinutes()));
    var saved = alertRepository.save(alert);
    saved.onRaised();
    alertRepository.save(saved);
    return saved;
  }
}
