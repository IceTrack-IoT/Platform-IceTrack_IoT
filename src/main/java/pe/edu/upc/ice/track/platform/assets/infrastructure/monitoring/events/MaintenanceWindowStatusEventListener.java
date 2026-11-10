package pe.edu.upc.ice.track.platform.assets.infrastructure.monitoring.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.EnterMaintenanceCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.LeaveMaintenanceCommand;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowClosedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowOpenedIntegrationEvent;

/**
 * Inbound infrastructure adapter that subscribes to the maintenance window integration events of
 * Monitoring and Alerting and keeps the unit's operational status in step with them.
 *
 * <p>An opened window moves the unit to {@code MAINTENANCE}, and its closing returns the unit to the
 * status it held before. Runs synchronously, inside the transaction that stored the window, so the
 * window and the status never disagree once committed.</p>
 */
@Component
public class MaintenanceWindowStatusEventListener {

  private final EquipmentCommandService equipmentCommandService;

  public MaintenanceWindowStatusEventListener(EquipmentCommandService equipmentCommandService) {
    this.equipmentCommandService = equipmentCommandService;
  }

  /**
   * @param event the integration event published when a window opens
   */
  @EventListener
  public void on(MaintenanceWindowOpenedIntegrationEvent event) {
    equipmentCommandService.handle(new EnterMaintenanceCommand(event.equipmentId(), event.maintenanceWindowId()));
  }

  /**
   * @param event the integration event published when a window closes
   */
  @EventListener
  public void on(MaintenanceWindowClosedIntegrationEvent event) {
    equipmentCommandService.handle(new LeaveMaintenanceCommand(event.equipmentId(), event.maintenanceWindowId()));
  }
}
