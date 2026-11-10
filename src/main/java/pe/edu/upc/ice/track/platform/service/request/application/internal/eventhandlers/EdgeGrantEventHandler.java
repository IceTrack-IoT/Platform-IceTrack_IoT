package pe.edu.upc.ice.track.platform.service.request.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.EdgeGrantIssuedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.EdgeGrantRevokedEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.EdgeGrantIssuedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.EdgeGrantRevokedIntegrationEvent;

/**
 * Internal application-layer handler translating the edge grant domain events into their
 * integration events.
 *
 * <p>Runs synchronously, inside the transaction that wrote the grant, so the unit's configuration
 * version moves in that same transaction and a gateway can never see a grant change under the
 * version it already holds.</p>
 */
@Service
public class EdgeGrantEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  public EdgeGrantEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * @param event the internal issuance event
   */
  @EventListener
  public void on(EdgeGrantIssuedEvent event) {
    eventPublisher.publishEvent(new EdgeGrantIssuedIntegrationEvent(
        event.serviceRequestId(), event.technicianProfileId(), event.equipmentId()));
  }

  /**
   * @param event the internal revocation event
   */
  @EventListener
  public void on(EdgeGrantRevokedEvent event) {
    eventPublisher.publishEvent(new EdgeGrantRevokedIntegrationEvent(
        event.serviceRequestId(), event.technicianProfileId(), event.equipmentId(), event.reason()));
  }
}
