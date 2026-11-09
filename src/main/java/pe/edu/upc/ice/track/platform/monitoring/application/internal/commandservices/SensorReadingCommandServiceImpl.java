package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.SensorReadingCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalDeviceServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.results.ReadingIngestionOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EnvironmentObservation;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;

/**
 * Default implementation of {@link SensorReadingCommandService}.
 *
 * <p>A batch never opens an alert: thermal excursions are opened by the edge gateway's events on
 * {@code POST /api/v1/telemetry/events}. A batch may only close the active excursion alert, as a
 * hysteresis-aware fallback for a normalization event that never arrives. A batch taken while a
 * maintenance window covered the unit is flagged {@code underMaintenance} and changes no alert.</p>
 *
 * <p>This is also where threshold evaluation lives: neither IAM nor Profiles introduce a
 * {@code domain.services} package in this codebase for cross-aggregate coordination, so, by the
 * same convention, that logic stays here rather than in a standalone domain service class. If
 * the team later prefers to extract it, it would become the first inhabitant of a new
 * {@code monitoring.domain.services} package.</p>
 *
 * <p>Every stored batch also refreshes the connectivity view other contexts display: the unit goes
 * online with the batch as its latest reading (through {@link ExternalAssetManagementService}), the
 * device's last contact moves to the batch's arrival (through
 * {@link ExternalDeviceServiceForMonitoring}), and an active {@code DEVICE_OFFLINE} alert of the unit
 * is resolved, because the unit is reporting again. A duplicate batch changes none of them.</p>
 *
 * <p>Ingestion is transactional: the reading, the connectivity updates, any alert it resolves, and
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
  private final ExternalDeviceServiceForMonitoring externalDeviceService;
  private final MaintenanceWindowRepository maintenanceWindowRepository;
  private final TransactionTemplate transactionTemplate;

  public SensorReadingCommandServiceImpl(
      SensorReadingRepository sensorReadingRepository,
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      ExternalAssetManagementService externalAssetManagementService,
      ExternalDeviceServiceForMonitoring externalDeviceService,
      MaintenanceWindowRepository maintenanceWindowRepository,
      PlatformTransactionManager transactionManager) {
    this.sensorReadingRepository = sensorReadingRepository;
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.externalAssetManagementService = externalAssetManagementService;
    this.externalDeviceService = externalDeviceService;
    this.maintenanceWindowRepository = maintenanceWindowRepository;
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

    // Judged by when the batch was taken, so a batch delivered late from the gateway's offline buffer
    // is still recognized as taken during maintenance.
    var underMaintenance = command.equipmentId() != null && command.recordedAt() != null
        && maintenanceWindowRepository.findCovering(
            command.equipmentId(), command.recordedAt()).isPresent();

    // Validated before any write: a batch outside the sensor's physical range is never stored.
    var reading = SensorReading.record(
        command.readingUid(), new EquipmentId(command.equipmentId()), command.deviceId(),
        command.minTemperature(), command.maxTemperature(), command.avgTemperature(),
        command.rawMinTemperature(), command.rawMaxTemperature(), command.rawAvgTemperature(),
        command.humidity(), command.sampleCount(),
        new EnvironmentObservation(command.lightLevelMax(), command.doorOpenSeconds(), command.presenceDetected()),
        command.recordedAt(), command.appliedConfigVersion(), underMaintenance);

    try {
      return transactionTemplate.execute(status -> {
        var savedReading = sensorReadingRepository.save(reading);
        refreshConnectivity(savedReading);
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
   * Reports a stored reading as the unit's and the device's latest sign of life, and resolves the
   * unit's active {@code DEVICE_OFFLINE} alert.
   *
   * <p>A batch taken during a maintenance window counts too: it still proves the unit is reporting.
   * The unit's last reading is judged by when the batch was taken, the device's last contact by when
   * the platform received it.</p>
   *
   * @param reading the reading that was just stored
   */
  private void refreshConnectivity(SensorReading reading) {
    var equipmentId = reading.getEquipmentId().equipmentId();
    externalAssetManagementService.recordEquipmentReading(
        equipmentId, reading.getAvgTemperature().celsius(), reading.getRecordedAt());
    externalDeviceService.recordDeviceActivity(reading.getDeviceId(), reading.getReceivedAt());
    alertRepository.findOpenAlertByEquipmentIdAndType(equipmentId, AlertType.DEVICE_OFFLINE)
        .ifPresent(offlineAlert -> {
          offlineAlert.resolveAutomatically();
          alertRepository.save(offlineAlert);
          log.info("Resolved offline alert {} of equipment {}: reading {} received",
              offlineAlert.getId(), equipmentId, reading.getReadingUid());
        });
  }

  /**
   * Evaluates a stored reading against the unit's active thermal excursion alert.
   *
   * <p>A reading batch never opens an alert: excursions are detected by the edge gateway, which
   * evaluates every sample against the sustained duration and reports them on
   * {@code POST /api/v1/telemetry/events}. Opening alerts here as well would raise a second alert
   * for the same excursion, from a single out-of-range batch.</p>
   *
   * <p>Batches are still the fallback that closes an excursion whose normalization event never
   * arrives. That resolution is hysteresis-aware, exactly like the gateway's: see
   * {@link #hasRecovered}.</p>
   *
   * <p>A batch taken during a maintenance window is not evaluated at all: the unit was being worked
   * on, so its temperatures say nothing about the excursion, and the active alert stays as it
   * was.</p>
   *
   * @param reading the reading that was just stored
   */
  private void evaluateThreshold(SensorReading reading) {
    var equipmentId = reading.getEquipmentId().equipmentId();
    if (reading.isUnderMaintenance()) {
      log.debug("Reading {} of equipment {} was taken during maintenance; not evaluated",
          reading.getReadingUid(), equipmentId);
      return;
    }
    var activeAlert = alertRepository.findOpenAlertByEquipmentIdAndType(equipmentId, AlertType.TEMPERATURE_EXCURSION);
    if (activeAlert.isEmpty()) {
      return;
    }
    var threshold = externalAssetManagementService.fetchTemperatureThreshold(equipmentId);
    if (threshold.isEmpty()) {
      log.warn("No temperature threshold found for equipment {}, skipping evaluation", equipmentId);
      return;
    }
    // The default is seeded at startup; the in-memory fallback only guards against a database in
    // which it was deleted, so evaluation never fails for lack of a policy.
    var policy = alertPolicyRepository.findApplicableTo(equipmentId)
        .orElseGet(() -> {
          log.warn("No alert policy stored for equipment {} nor a platform default; using built-in defaults",
              equipmentId);
          return AlertPolicy.platformDefault();
        });
    var alert = activeAlert.get();
    if (hasRecovered(alert, reading, threshold.get()[0], threshold.get()[1], policy)) {
      alert.resolveAutomatically();
      alertRepository.save(alert);
      log.info("Resolved alert {} of equipment {} from stored readings", alert.getId(), equipmentId);
    }
  }

  /**
   * Tells whether the stored readings show that an excursion is over, with the same rule the edge
   * gateway applies before reporting a normalization.
   *
   * <p>Every batch recorded during the last {@code sustainedExcursionMinutes}, up to the latest one,
   * must lie back within range by at least the hysteresis margin on the side that was breached:
   * below {@code max - margin} after an excursion above the range, above {@code min + margin} after
   * one below it, and inside the plain range on the other side. When the breached side cannot be
   * told - an old alert without a triggering temperature, or a threshold changed since - both sides
   * are narrowed. The window must start after the excursion did, and hold at least one batch per
   * minute of it, so a gap in the telemetry is never mistaken for a recovery.</p>
   *
   * @param alert      the active excursion alert
   * @param latest     the reading that was just stored
   * @param minCelsius the lowest acceptable temperature
   * @param maxCelsius the highest acceptable temperature
   * @param policy     the policy applicable to the unit
   * @return {@code true} when the alert can be resolved
   */
  private boolean hasRecovered(Alert alert, SensorReading latest, double minCelsius, double maxCelsius,
                               AlertPolicy policy) {
    var sustainedMinutes = policy.getSustainedExcursionMinutes() == null ? 0 : policy.getSustainedExcursionMinutes();
    var windowEnd = latest.getRecordedAt();
    var windowStart = windowEnd.minus(Duration.ofMinutes(sustainedMinutes));
    var excursionStart = alert.getTriggeringRecordedAt() != null ? alert.getTriggeringRecordedAt() : alert.getOpenedAt();
    if (excursionStart != null && !windowStart.isAfter(excursionStart)) {
      return false;
    }
    // Batches taken during maintenance are left out: they neither prove nor disprove a recovery.
    var window = sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(
            latest.getEquipmentId().equipmentId(), windowStart, windowEnd).stream()
        .filter(candidate -> !candidate.isUnderMaintenance())
        .toList();
    if (window.size() < Math.max(1, sustainedMinutes)) {
      return false;
    }

    var margin = policy.getHysteresisMargin().celsius() == null ? 0.0 : policy.getHysteresisMargin().celsius();
    var reference = alert.getTriggeringTemperature().celsius() != null
        ? alert.getTriggeringTemperature().celsius()
        : alert.getPeakTemperature().celsius();
    var breachedMaximum = reference != null && reference > maxCelsius;
    var breachedMinimum = reference != null && reference < minCelsius;
    var sideUnknown = !breachedMaximum && !breachedMinimum;
    var lowerBound = breachedMinimum || sideUnknown ? minCelsius + margin : minCelsius;
    var upperBound = breachedMaximum || sideUnknown ? maxCelsius - margin : maxCelsius;

    return window.stream().allMatch(candidate -> {
      var min = candidate.getMinTemperature().celsius();
      var max = candidate.getMaxTemperature().celsius();
      return min != null && max != null && min >= lowerBound && max <= upperBound;
    });
  }
}
