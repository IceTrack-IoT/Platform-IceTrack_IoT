package pe.edu.upc.ice.track.platform.assets.infrastructure.monitoring.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.BumpConfigVersionCommand;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.AlertPolicyUpdatedIntegrationEvent;

/**
 * Inbound infrastructure adapter that subscribes to {@link AlertPolicyUpdatedIntegrationEvent},
 * published by Monitoring and Alerting, and increments the unit's edge configuration version.
 *
 * <p>The unit's gateway enforces the alert policy locally, and only downloads its configuration
 * again when the version moved. Runs synchronously, inside the transaction that saved the policy.</p>
 */
@Component
public class AlertPolicyUpdatedEventListener {

  private final EquipmentCommandService equipmentCommandService;

  public AlertPolicyUpdatedEventListener(EquipmentCommandService equipmentCommandService) {
    this.equipmentCommandService = equipmentCommandService;
  }

  /**
   * @param event the integration event published by Monitoring and Alerting
   */
  @EventListener
  public void on(AlertPolicyUpdatedIntegrationEvent event) {
    if (event.equipmentId() == null) {
      return;
    }
    equipmentCommandService.handle(new BumpConfigVersionCommand(event.equipmentId(), "alert policy updated"));
  }
}
