package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;

/**
 *  Implementation of the AlertPolicyCommandService interface that handles commands related to alert policies.
 */
@Service
public class AlertPolicyCommandServiceImpl implements AlertPolicyCommandService {

  /**
   * Repository for managing AlertPolicy entities.
   */
  private final AlertPolicyRepository alertPolicyRepository;

  public AlertPolicyCommandServiceImpl(AlertPolicyRepository alertPolicyRepository) {
    this.alertPolicyRepository = alertPolicyRepository;
  }

  /**
   *  Handles the UpdateAlertPolicyCommand by either updating an existing alert policy or creating a new one if it doesn't exist.
   * @param command the command containing the update information
   * @return  the updated or newly created AlertPolicy
   */
  @Override
  public AlertPolicy handle(UpdateAlertPolicyCommand command) {
    var policy = alertPolicyRepository.findByEquipmentId(command.equipmentId())
        .orElseGet(() -> new AlertPolicy(
            new EquipmentId(command.equipmentId()), command.sustainedExcursionMinutes(),
            command.hysteresisMarginCelsius(), command.missedSyncWindowsForOffline()));
    policy.updatePolicy(
        command.sustainedExcursionMinutes(), command.hysteresisMarginCelsius(),
        command.missedSyncWindowsForOffline());
    return alertPolicyRepository.save(policy);
  }
}
