package pe.edu.upc.ice.track.platform.service.request.application.internal.eventhandlers;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ReviewSubmittedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ReviewUpdatedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceCompletedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceRequestCanceledEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.ServiceRequestRejectedEvent;
import pe.edu.upc.ice.track.platform.service.request.domain.model.events.TechnicianAssignedEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ReviewSubmittedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ReviewUpdatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestCanceledIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestCompletedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestRejectedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.TechnicianAssignedIntegrationEvent;
import pe.edu.upc.ice.track.platform.shared.application.outbox.IntegrationEventOutbox;

/**
 * Internal application-layer handler translating the service request and review domain events into
 * integration events stored in the transactional outbox.
 *
 * <p>Runs synchronously, inside the transaction that saved the request or the review - the
 * repositories publish domain events right after flushing - so each integration event is stored if
 * and only if its change commits. The outbox dispatcher delivers them afterward, at least once,
 * without the request ever waiting for its consumers: the technician metrics projection in Profiles
 * and the notifications of the parties.</p>
 */
@Service
public class ServiceRequestOutboxEventHandler {

  private static final String SERVICE_REQUEST = "ServiceRequest";
  private static final String REVIEW = "Review";

  private final IntegrationEventOutbox outbox;

  public ServiceRequestOutboxEventHandler(IntegrationEventOutbox outbox) {
    this.outbox = outbox;
  }

  /**
   * @param event the internal assignment event
   */
  @EventListener
  public void on(TechnicianAssignedEvent event) {
    outbox.append(SERVICE_REQUEST, event.serviceRequestId(), new TechnicianAssignedIntegrationEvent(
        event.serviceRequestId(),
        event.ownerId(),
        event.technicianId(),
        event.previousTechnicianId(),
        event.equipmentId(),
        event.siteId(),
        event.type().name(),
        event.priority().name(),
        event.assignedAt()));
  }

  /**
   * @param event the internal rejection event
   */
  @EventListener
  public void on(ServiceRequestRejectedEvent event) {
    outbox.append(SERVICE_REQUEST, event.serviceRequestId(), new ServiceRequestRejectedIntegrationEvent(
        event.serviceRequestId(),
        event.ownerId(),
        event.technicianId(),
        event.equipmentId(),
        event.reason(),
        event.rejectedAt()));
  }

  /**
   * @param event the internal cancellation event
   */
  @EventListener
  public void on(ServiceRequestCanceledEvent event) {
    outbox.append(SERVICE_REQUEST, event.serviceRequestId(), new ServiceRequestCanceledIntegrationEvent(
        event.serviceRequestId(),
        event.ownerId(),
        event.technicianId(),
        event.equipmentId(),
        event.reason(),
        event.canceledAt()));
  }

  /**
   * @param event the internal completion event
   */
  @EventListener
  public void on(ServiceCompletedEvent event) {
    outbox.append(SERVICE_REQUEST, event.serviceRequestId(), new ServiceRequestCompletedIntegrationEvent(
        event.serviceRequestId(),
        event.ownerId(),
        event.technicianId(),
        event.equipmentId(),
        event.completedAt()));
  }

  /**
   * @param event the internal review submission event
   */
  @EventListener
  public void on(ReviewSubmittedEvent event) {
    outbox.append(REVIEW, event.reviewId(), new ReviewSubmittedIntegrationEvent(
        event.reviewId(),
        event.serviceRequestId(),
        event.technicianId(),
        event.averageRating(),
        event.submittedAt()));
  }

  /**
   * @param event the internal review edit event
   */
  @EventListener
  public void on(ReviewUpdatedEvent event) {
    outbox.append(REVIEW, event.reviewId(), new ReviewUpdatedIntegrationEvent(
        event.reviewId(),
        event.serviceRequestId(),
        event.technicianId(),
        event.averageRating(),
        event.updatedAt()));
  }
}
