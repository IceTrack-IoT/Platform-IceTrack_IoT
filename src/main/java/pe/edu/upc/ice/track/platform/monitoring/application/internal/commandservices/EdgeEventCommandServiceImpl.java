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
 * cloud no longer opens alerts from single reading batches. They also open, promote and close the
 * unit's maintenance windows, through {@link MaintenanceWindowEventProcessor}; a detection dated
 * inside a window opens no alert.</p>
 *
 * <p>Idempotency works like reading ingestion. An event whose {@code eventUid} is already recorded
 * is answered as a duplicate and nothing is processed. Two concurrent deliveries of the same event
 * can both pass that check; the unique index on {@code event_uid} rejects the second insert, its
 * transaction - alert or window change included - is rolled back, and it is answered as a duplicate
 * too. That is why the work runs in a {@link TransactionTemplate}: the violation must be caught after
 * the rollback, outside the transaction.</p>
 */
@Service
@Slf4j
public class EdgeEventCommandServiceImpl implements EdgeEventCommandService {

  private final EdgeEventRepository edgeEventRepository;
  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final SensorReadingRepository sensorReadingRepository;
  private final MaintenanceWindowEventProcessor maintenanceWindowEventProcessor;
  private final TransactionTemplate transactionTemplate;

  public EdgeEventCommandServiceImpl(
      EdgeEventRepository edgeEventRepository,
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      SensorReadingRepository sensorReadingRepository,
      MaintenanceWindowEventProcessor maintenanceWindowEventProcessor,
      PlatformTransactionManager transactionManager) {
    this.edgeEventRepository = edgeEventRepository;
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.sensorReadingRepository = sensorReadingRepository;
    this.maintenanceWindowEventProcessor = maintenanceWindowEventProcessor;
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
   * Applies the event to the unit's active excursion alert or maintenance window and records it, in
   * the caller's transaction.
   *
   * @param command the event
   * @return the stored record of the event
   */
  private EdgeEvent process(RecordEdgeEventCommand command) {
    return switch (command.type()) {
      case THERMAL_EXCURSION_DETECTED, THERMAL_EXCURSION_NORMALIZED -> processExcursion(command);
      case MAINTENANCE_WINDOW_STARTED -> storeWindowEvent(command, maintenanceWindowEventProcessor.start(command));
      case MAINTENANCE_WINDOW_ENDED -> storeWindowEvent(command, maintenanceWindowEventProcessor.end(command));
    };
  }

  /**
   * Records a maintenance window event with what it did to the unit's window.
   *
   * @param command the event
   * @param outcome what the window processing did
   * @return the stored record of the event
   */
  private EdgeEvent storeWindowEvent(RecordEdgeEventCommand command, MaintenanceWindowEventProcessor.Outcome outcome) {
    return store(command, outcome.result(), null, outcome.maintenanceWindowId());
  }

  /**
   * Applies an excursion event to the unit's active excursion alert.
   *
   * <p>A detection dated inside a maintenance window - even one delivered late from the gateway's
   * offline buffer - opens nothing: the unit was being worked on, so the excursion is expected. The
   * active alert, if any, is left exactly as it was.</p>
   *
   * @param command the excursion event
   * @return the stored record of the event
   */
  private EdgeEvent processExcursion(RecordEdgeEventCommand command) {
    if (command.type() == EdgeEventType.THERMAL_EXCURSION_DETECTED) {
      var coveringWindow = maintenanceWindowEventProcessor.findCovering(command.equipmentId(), command.occurredAt());
      if (coveringWindow.isPresent()) {
        return store(command, EdgeEventResult.SUPPRESSED_BY_MAINTENANCE, null, coveringWindow.get().getId());
      }
    }
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
    return store(command, result, alertId, null);
  }

  /**
   * Stores the record of a processed event.
   *
   * @param command             the event
   * @param result              what processing did
   * @param alertId             the alert concerned, or {@code null}
   * @param maintenanceWindowId the maintenance window concerned, or {@code null}
   * @return the stored record
   */
  private EdgeEvent store(RecordEdgeEventCommand command, EdgeEventResult result, Long alertId,
                          Long maintenanceWindowId) {
    var stored = edgeEventRepository.save(new EdgeEvent(
        null, command.eventUid(), command.deviceId(), command.equipmentId(), command.type(), command.occurredAt(),
        command.temperatureCelsius(), command.peakTemperatureCelsius(), result, alertId, maintenanceWindowId));
    log.info("Edge event {} ({}) from device {} on equipment {}: {} (alert {}, maintenance window {})",
        stored.getEventUid(), stored.getType(), stored.getDeviceId(), stored.getEquipmentId(), result, alertId,
        maintenanceWindowId);
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
