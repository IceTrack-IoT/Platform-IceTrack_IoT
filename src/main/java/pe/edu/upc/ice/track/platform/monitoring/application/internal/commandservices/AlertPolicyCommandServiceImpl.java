package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.AlertPolicyCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;

@Service
public class AlertPolicyCommandServiceImpl implements AlertPolicyCommandService {

  private final AlertPolicyRepository alertPolicyRepository;

  public AlertPolicyCommandServiceImpl(AlertPolicyRepository alertPolicyRepository) {
    this.alertPolicyRepository = alertPolicyRepository;
  }

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
