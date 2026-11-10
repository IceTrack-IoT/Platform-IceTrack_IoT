package pe.edu.upc.ice.track.platform.assets.infrastructure.servicerequest.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.EquipmentCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.BumpConfigVersionCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.EdgeGrantIssuedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.EdgeGrantRevokedIntegrationEvent;

/**
 * Inbound infrastructure adapter that subscribes to the edge grant integration events of Service
 * Request Management and increments the unit's edge configuration version.
 *
 * <p>The gateway's configuration lists the technician grants of its unit, so issuing or revoking
 * one must move the version for the gateway to download it again. Runs synchronously, inside the
 * transaction that wrote the grant.</p>
 */
@Component
public class EdgeGrantEventListener {

  private final EquipmentCommandService equipmentCommandService;

  public EdgeGrantEventListener(EquipmentCommandService equipmentCommandService) {
    this.equipmentCommandService = equipmentCommandService;
  }

  /**
   * @param event the integration event published when a grant is issued
   */
  @EventListener
  public void on(EdgeGrantIssuedIntegrationEvent event) {
    equipmentCommandService.handle(new BumpConfigVersionCommand(
        event.equipmentId(), "edge grant issued for service request " + event.serviceRequestId()));
  }

  /**
   * @param event the integration event published when a grant is revoked
   */
  @EventListener
  public void on(EdgeGrantRevokedIntegrationEvent event) {
    equipmentCommandService.handle(new BumpConfigVersionCommand(
        event.equipmentId(), "edge grant of service request %s revoked (%s)"
            .formatted(event.serviceRequestId(), event.reason())));
  }
}
