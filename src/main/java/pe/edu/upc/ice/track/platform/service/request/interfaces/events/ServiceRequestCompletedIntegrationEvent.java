package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when the assigned technician registers the intervention that completes
 * a service request.
 *
 * <p>The published language of the {@code service.request} context for this fact. Delivered through
 * the transactional outbox, at least once: Profiles counts the technician's completed services in its
 * metrics read model, keyed by {@code serviceRequestId} so a redelivery is not counted twice.</p>
 *
 * @param serviceRequestId    the completed request
 * @param ownerId             the owner of the request
 * @param technicianProfileId the technician who completed it
 * @param equipmentId         the unit that was serviced
 * @param completedAt         when the work finished
 */
public record ServiceRequestCompletedIntegrationEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianProfileId,
    Long equipmentId,
    Instant completedAt) {
}
