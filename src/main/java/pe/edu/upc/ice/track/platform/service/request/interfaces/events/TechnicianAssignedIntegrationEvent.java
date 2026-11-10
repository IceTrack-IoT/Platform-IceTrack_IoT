package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when an owner assigns a technician to a service request, including a
 * reassignment that replaces the previous technician.
 *
 * <p>Delivered through the transactional outbox: Notifications tells the new technician, and the
 * outgoing one when there was one.</p>
 *
 * @param serviceRequestId            the request
 * @param ownerId                     the owner who assigned
 * @param technicianProfileId         the technician now assigned
 * @param previousTechnicianProfileId the technician replaced, {@code null} when there was none
 * @param equipmentId                 the unit to service
 * @param siteId                      the site the unit is installed at
 * @param serviceType                 the kind of work, as plain text
 * @param priority                    the urgency, as plain text
 * @param assignedAt                  when the assignment happened
 */
public record TechnicianAssignedIntegrationEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianProfileId,
    Long previousTechnicianProfileId,
    Long equipmentId,
    Long siteId,
    String serviceType,
    String priority,
    Instant assignedAt) {
}
