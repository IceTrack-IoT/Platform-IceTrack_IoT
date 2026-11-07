package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowClosedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowOpenedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowPromotedEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowClosedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowOpenedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowPromotedIntegrationEvent;

/**
 * Internal application-layer handler translating the maintenance window domain events into their
 * integration events.
 *
 * <p>Runs synchronously, inside the transaction that stored the window, so the unit's operational
 * status moves in that same transaction. Consumers that must not affect the window - the service
 * request link and the owner's notification - subscribe after the commit.</p>
 */
@Service
public class MaintenanceWindowEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  public MaintenanceWindowEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * @param event the internal opening event
   */
  @EventListener
  public void on(MaintenanceWindowOpenedEvent event) {
    eventPublisher.publishEvent(new MaintenanceWindowOpenedIntegrationEvent(
        event.maintenanceWindowId(),
        event.equipmentId(),
        event.origin().name(),
        event.startedAt(),
        event.expiresAt(),
        event.technicianProfileId(),
        event.serviceRequestId()));
  }

  /**
   * @param event the internal promotion event
   */
  @EventListener
  public void on(MaintenanceWindowPromotedEvent event) {
    eventPublisher.publishEvent(new MaintenanceWindowPromotedIntegrationEvent(
        event.maintenanceWindowId(),
        event.equipmentId(),
        event.startedAt(),
        event.promotedAt(),
        event.expiresAt(),
        event.technicianProfileId(),
        event.serviceRequestId()));
  }

  /**
   * @param event the internal closing event
   */
  @EventListener
  public void on(MaintenanceWindowClosedEvent event) {
    eventPublisher.publishEvent(new MaintenanceWindowClosedIntegrationEvent(
        event.maintenanceWindowId(),
        event.equipmentId(),
        event.origin().name(),
        event.startedAt(),
        event.endedAt(),
        event.closureReason().name(),
        event.technicianProfileId(),
        event.serviceRequestId()));
  }
}
