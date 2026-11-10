package pe.edu.upc.ice.track.platform.notifications.infrastructure.servicerequest.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestCanceledCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyServiceRequestRejectedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyTechnicianAssignedCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestCanceledIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestRejectedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.TechnicianAssignedIntegrationEvent;

/**
 * Inbound infrastructure adapter that subscribes to the service request lifecycle integration events
 * of Service Request Management and notifies the parties.
 *
 * <p>This is the only class in {@code notifications} that knows a {@code service.request} context
 * exists. The events are delivered asynchronously by the transactional outbox, after the request's
 * transaction committed, and this adapter runs inside the delivery's transaction: when it fails, the
 * notification it was writing is rolled back and the event is delivered again later.</p>
 *
 * <ul>
 *   <li>an assignment notifies the new technician - unit, site, priority and date - and the
 *       technician it replaced, if any;</li>
 *   <li>a rejection notifies the owner with the technician's reason;</li>
 *   <li>a cancellation notifies the technician who was assigned, if any, with the owner's reason.</li>
 * </ul>
 */
@Component
@Slf4j
public class ServiceRequestNotificationEventListener {

  private final NotificationCommandService notificationCommandService;

  public ServiceRequestNotificationEventListener(NotificationCommandService notificationCommandService) {
    this.notificationCommandService = notificationCommandService;
  }

  /**
   * @param event the integration event delivered from the outbox
   */
  @EventListener
  public void on(TechnicianAssignedIntegrationEvent event) {
    notificationCommandService.handle(new NotifyTechnicianAssignedCommand(
        event.serviceRequestId(),
        event.technicianProfileId(),
        event.previousTechnicianProfileId(),
        event.equipmentId(),
        event.siteId(),
        event.serviceType(),
        event.priority(),
        event.assignedAt()));
  }

  /**
   * @param event the integration event delivered from the outbox
   */
  @EventListener
  public void on(ServiceRequestRejectedIntegrationEvent event) {
    notificationCommandService.handle(new NotifyServiceRequestRejectedCommand(
        event.serviceRequestId(),
        event.ownerId(),
        event.technicianProfileId(),
        event.equipmentId(),
        event.reason()));
  }

  /**
   * Notifies the technician of a canceled request; a request canceled before anybody was assigned
   * notifies nobody.
   *
   * @param event the integration event delivered from the outbox
   */
  @EventListener
  public void on(ServiceRequestCanceledIntegrationEvent event) {
    if (event.technicianProfileId() == null) {
      log.debug("Service request {} was canceled with no technician assigned; nobody to notify",
          event.serviceRequestId());
      return;
    }
    notificationCommandService.handle(new NotifyServiceRequestCanceledCommand(
        event.serviceRequestId(),
        event.technicianProfileId(),
        event.equipmentId(),
        event.reason()));
  }
}
