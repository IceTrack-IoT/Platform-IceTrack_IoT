package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.SensorReadingCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.results.ReadingIngestionOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EnvironmentObservation;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;

/**
 * Default implementation of {@link SensorReadingCommandService}.
 *
 * <p>This is also where threshold evaluation lives: neither IAM nor Profiles introduce a
 * {@code domain.services} package in this codebase for cross-aggregate coordination, so, by the
 * same convention, that logic stays here rather than in a standalone domain service class. If
 * the team later prefers to extract it, it would become the first inhabitant of a new
 * {@code monitoring.domain.services} package.</p>
 *
 * <p>Ingestion is transactional: the reading, any alert it opens or resolves, and
 * whatever the synchronous listeners of those alerts write commit together or not at all. A
 * failure after the reading row is written therefore no longer leaves a stored reading whose
 * retries are all discarded as duplicates, with its threshold evaluation lost.</p>
 *
 * <p>Idempotency: a batch whose {@code readingUid} is already stored is answered as a duplicate
 * with the stored reading, and nothing is written. Two concurrent deliveries of the same batch can
 * both pass that check; the unique index on {@code reading_uid} rejects the second insert, its
 * transaction is rolled back, and it is answered as a duplicate too, instead of as a conflict.
 * That is why the write runs in a {@link TransactionTemplate} rather than under
 * {@code @Transactional}: the violation must be caught after the rollback, outside the
 * transaction.</p>
 */
@Service
@Slf4j
public class SensorReadingCommandServiceImpl implements SensorReadingCommandService {

  private final SensorReadingRepository sensorReadingRepository;
  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final ExternalAssetManagementService externalAssetManagementService;
  private final TransactionTemplate transactionTemplate;

  public SensorReadingCommandServiceImpl(
      SensorReadingRepository sensorReadingRepository,
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      ExternalAssetManagementService externalAssetManagementService,
      PlatformTransactionManager transactionManager) {
    this.sensorReadingRepository = sensorReadingRepository;
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.externalAssetManagementService = externalAssetManagementService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  // inherited javadoc
  @Override
  public ReadingIngestionOutcome handle(RecordReadingBatchCommand command) {
    var existing = sensorReadingRepository.findByReadingUid(command.readingUid());
    if (existing.isPresent()) {
      log.info("Duplicate reading {} from device {}", command.readingUid(), command.deviceId());
      return new ReadingIngestionOutcome.Duplicate(existing.get());
    }

    // Validated before any write: a batch outside the sensor's physical range is never stored.
    var reading = SensorReading.record(
        command.readingUid(), new EquipmentId(command.equipmentId()), command.deviceId(),
        command.minTemperature(), command.maxTemperature(), command.avgTemperature(),
        command.rawMinTemperature(), command.rawMaxTemperature(), command.rawAvgTemperature(),
        command.humidity(), command.sampleCount(),
        new EnvironmentObservation(command.lightLevelMax(), command.doorOpenSeconds(), command.presenceDetected()),
        command.recordedAt(), command.appliedConfigVersion());

    try {
      return transactionTemplate.execute(status -> {
        var savedReading = sensorReadingRepository.save(reading);
        evaluateThreshold(savedReading);
        return new ReadingIngestionOutcome.Recorded(savedReading);
      });
    } catch (DataIntegrityViolationException e) {
      return sensorReadingRepository.findByReadingUid(command.readingUid())
          .<ReadingIngestionOutcome>map(stored -> {
            log.info("Concurrent duplicate reading {} from device {}", command.readingUid(), command.deviceId());
            return new ReadingIngestionOutcome.Duplicate(stored);
          })
          .orElseThrow(() -> e);
    }
  }

  /**
   *  Evaluates the temperature thresholds for the given reading and raises or resolves alerts as needed.
   * @param reading The {@link SensorReading} instance to evaluate
   */
  private void evaluateThreshold(SensorReading reading) {
    var thresholdOpt = externalAssetManagementService.fetchTemperatureThreshold(reading.getEquipmentId().equipmentId());
    if (thresholdOpt.isEmpty()) {
      log.warn("No temperature threshold found for equipment {}, skipping evaluation",
          reading.getEquipmentId());
      return;
    }
    var threshold = thresholdOpt.get();
    var minThreshold = new Temperature(threshold[0]);
    var maxThreshold = new Temperature(threshold[1]);

    // The default is seeded at startup; the in-memory fallback only guards against a database in
    // which it was deleted, so evaluation never fails for lack of a policy.
    var policy = alertPolicyRepository.findApplicableTo(reading.getEquipmentId().equipmentId())
        .orElseGet(() -> {
          log.warn("No alert policy stored for equipment {} nor a platform default; using built-in defaults",
              reading.getEquipmentId().equipmentId());
          return AlertPolicy.platformDefault();
        });

    var outOfRange = reading.getMaxTemperature().isAbove(maxThreshold)
        || reading.getMinTemperature().isBelow(minThreshold);

    var existingAlert = alertRepository.findOpenAlertByEquipmentIdAndType(
        reading.getEquipmentId().equipmentId(), AlertType.TEMPERATURE_EXCURSION);

    if (outOfRange && existingAlert.isEmpty()) {
      var alert = new Alert(
          reading.getEquipmentId().equipmentId(), AlertType.TEMPERATURE_EXCURSION, AlertSeverity.WARNING,
          reading.getId(), reading.getMaxTemperature().celsius(),
          Duration.ofMinutes(policy.getSustainedExcursionMinutes()));
      var savedAlert = alertRepository.save(alert);
      savedAlert.onRaised();
      alertRepository.save(savedAlert);
    } else if (!outOfRange && existingAlert.isPresent()) {
      var alert = existingAlert.get();
      alert.resolve();
      alertRepository.save(alert);
    }
  }
}
