package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.EquipmentConnectivityCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DetectOfflineEquipmentCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Instant;

/**
 * Default implementation of {@link EquipmentConnectivityCommandService}.
 *
 * <p>The candidates are the units Assets Management shows as connected; silence is measured from the
 * arrival of each unit's latest stored reading, by the server clock. Raising the alert goes through
 * {@link Alert#raisedForSilence} and {@link Alert#onRaised}, exactly like a thermal alert, so the
 * {@code AlertRaisedIntegrationEvent} that notifies the owner and the stream update are published the
 * same way.</p>
 */
@Service
@Slf4j
public class EquipmentConnectivityCommandServiceImpl implements EquipmentConnectivityCommandService {

  private final SensorReadingRepository sensorReadingRepository;
  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;
  private final MaintenanceWindowRepository maintenanceWindowRepository;
  private final ExternalAssetManagementService externalAssetManagementService;
  private final TransactionTemplate transactionTemplate;

  public EquipmentConnectivityCommandServiceImpl(
      SensorReadingRepository sensorReadingRepository,
      AlertRepository alertRepository,
      AlertPolicyRepository alertPolicyRepository,
      MaintenanceWindowRepository maintenanceWindowRepository,
      ExternalAssetManagementService externalAssetManagementService,
      PlatformTransactionManager transactionManager) {
    this.sensorReadingRepository = sensorReadingRepository;
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
    this.maintenanceWindowRepository = maintenanceWindowRepository;
    this.externalAssetManagementService = externalAssetManagementService;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  // inherited javadoc
  @Override
  public int handle(DetectOfflineEquipmentCommand command) {
    var raised = 0;
    for (var equipmentId : externalAssetManagementService.fetchOnlineEquipmentIds()) {
      try {
        if (Boolean.TRUE.equals(transactionTemplate.execute(status -> detect(equipmentId, command.detectedAt())))) {
          raised++;
        }
      } catch (RuntimeException exception) {
        log.error("Offline detection failed for equipment {}: {}", equipmentId, exception.getMessage(), exception);
      }
    }
    return raised;
  }

  /**
   * Judges one connected unit.
   *
   * @param equipmentId the unit
   * @param now         the instant to judge at
   * @return {@code true} when a {@code DEVICE_OFFLINE} alert was raised
   */
  private boolean detect(Long equipmentId, Instant now) {
    var latest = sensorReadingRepository.findLatestReceivedByEquipmentId(equipmentId);
    if (latest.isEmpty()) {
      return false;
    }
    var silentSince = latest.get().getReceivedAt();
    var policy = alertPolicyRepository.findApplicableTo(equipmentId).orElseGet(AlertPolicy::platformDefault);
    if (!policy.isOfflineSince(silentSince, now)) {
      return false;
    }
    if (maintenanceWindowRepository.findCovering(equipmentId, now).isPresent()) {
      log.debug("Equipment {} is silent since {} but under maintenance; no offline alert", equipmentId, silentSince);
      return false;
    }
    externalAssetManagementService.markEquipmentOffline(equipmentId);
    if (alertRepository.findOpenAlertByEquipmentIdAndType(equipmentId, AlertType.DEVICE_OFFLINE).isPresent()) {
      return false;
    }
    var saved = alertRepository.save(Alert.raisedForSilence(equipmentId, silentSince));
    saved.onRaised();
    alertRepository.save(saved);
    log.info("Raised offline alert {} for equipment {}: no reading received since {}",
        saved.getId(), equipmentId, silentSince);
    return true;
  }
}
