package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.SeedDefaultAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

/**
 * Implementation of the AlertPolicyCommandService interface that handles commands related to alert policies.
 *
 * <p>Only per-equipment policies are writable, and only by the owner of the equipment. The
 * platform-wide default is seeded at startup and is never writable over REST.</p>
 */
@Service
@Slf4j
public class AlertPolicyCommandServiceImpl implements AlertPolicyCommandService {

  private static final String EQUIPMENT_RESOURCE = "Equipment";
  private static final String ALERT_POLICY_RESOURCE = "AlertPolicy";

  private final AlertPolicyRepository alertPolicyRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;

  public AlertPolicyCommandServiceImpl(
      AlertPolicyRepository alertPolicyRepository, EquipmentAccessPolicy equipmentAccessPolicy) {
    this.alertPolicyRepository = alertPolicyRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
  }

  /**
   * Updates the policy of one of the caller's units, creating it when the unit has none yet.
   * @param command the command containing the update information
   * @return the updated or newly created AlertPolicy, or the failure explaining why it was refused
   */
  @Override
  @Transactional
  public Result<AlertPolicy, ApplicationError> handle(UpdateAlertPolicyCommand command) {
    if (command.equipmentId() == null) {
      return Result.failure(ApplicationError.accessDenied(
          "The platform-wide default alert policy is managed by configuration"));
    }
    if (!equipmentAccessPolicy.canManage(command.equipmentId(), command.username())) {
      return Result.failure(ApplicationError.notFound(EQUIPMENT_RESOURCE, String.valueOf(command.equipmentId())));
    }
    try {
      var policy = alertPolicyRepository.findByEquipmentId(command.equipmentId())
          .orElseGet(() -> new AlertPolicy(
              new EquipmentId(command.equipmentId()), command.sustainedExcursionMinutes(),
              command.hysteresisMarginCelsius(), command.missedSyncWindowsForOffline()));
      policy.updatePolicy(
          command.sustainedExcursionMinutes(), command.hysteresisMarginCelsius(),
          command.missedSyncWindowsForOffline());
      return Result.success(alertPolicyRepository.save(policy));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(ALERT_POLICY_RESOURCE, e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public void handle(SeedDefaultAlertPolicyCommand command) {
    if (alertPolicyRepository.findDefaultPolicy().isPresent()) {
      return;
    }
    alertPolicyRepository.save(AlertPolicy.platformDefault());
    log.info("Seeded the platform-wide default alert policy ({} min sustained, {} C hysteresis, {} missed windows)",
        AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES, AlertPolicy.DEFAULT_HYSTERESIS_MARGIN_CELSIUS,
        AlertPolicy.DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE);
  }
}
