package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.SensorReadingCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordReadingBatchCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;
import java.util.Optional;

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
 */
@Service
@Slf4j
public class SensorReadingCommandServiceImpl implements SensorReadingCommandService {

  private final SensorReadingRepository sensorReadingRepository;
  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final ExternalAssetManagementService externalAssetManagementService;

  public SensorReadingCommandServiceImpl(
      SensorReadingRepository sensorReadingRepository,
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      ExternalAssetManagementService externalAssetManagementService) {
    this.sensorReadingRepository = sensorReadingRepository;
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.externalAssetManagementService = externalAssetManagementService;
  }

  /**
   *  Records a new sensor reading batch, de-duplicating by {@code readingUid}, and evaluates
   * @param command The {@link RecordReadingBatchCommand} instance
   * @return  An {@link Optional} containing the saved {@link SensorReading} if it was new, or empty if
   */
  @Override
  @Transactional
  public Optional<SensorReading> handle(RecordReadingBatchCommand command) {
    if (sensorReadingRepository.findByReadingUid(command.readingUid()).isPresent()) {
      log.info("Ignoring duplicate reading {}", command.readingUid());
      return Optional.empty();
    }

    var reading = new SensorReading(
        command.readingUid(), new EquipmentId(command.equipmentId()), command.deviceId(),
        command.minTemperature(), command.maxTemperature(), command.avgTemperature(),
        command.humidity(), command.sampleCount(), command.recordedAt());
    var savedReading = sensorReadingRepository.save(reading);

    evaluateThreshold(savedReading);

    return Optional.of(savedReading);
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
