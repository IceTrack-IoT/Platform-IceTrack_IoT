package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when the assigned technician rejects a service request.
 *
 * <p>Delivered through the transactional outbox: Notifications tells the owner, with the reason, so it
 * can assign somebody else.</p>
 *
 * @param serviceRequestId    the request
 * @param ownerId             the owner of the request
 * @param technicianProfileId the technician who rejected
 * @param equipmentId         the unit to service
 * @param reason              why the technician rejected
 * @param rejectedAt          when the request was rejected
 */
public record ServiceRequestRejectedIntegrationEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianProfileId,
    Long equipmentId,
    String reason,
    Instant rejectedAt) {
}
