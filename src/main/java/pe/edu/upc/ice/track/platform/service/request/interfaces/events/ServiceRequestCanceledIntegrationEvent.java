package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when an owner cancels a service request.
 *
 * <p>Delivered through the transactional outbox: Notifications tells the technician who was assigned,
 * if any, with the reason.</p>
 *
 * @param serviceRequestId    the request
 * @param ownerId             the owner who canceled
 * @param technicianProfileId the technician assigned at the time, {@code null} when there was none
 * @param equipmentId         the unit the request was raised for
 * @param reason              why the owner canceled
 * @param canceledAt          when the request was canceled
 */
public record ServiceRequestCanceledIntegrationEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianProfileId,
    Long equipmentId,
    String reason,
    Instant canceledAt) {
}
