package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;

import java.time.Instant;

/**
 * Domain event published when the assigned technician rejects a service request, which leaves the
 * request waiting for the owner to assign somebody else.
 *
 * @param serviceRequestId the request
 * @param ownerId          the owner of the request, who now has to reassign it
 * @param technicianId     the technician who rejected
 * @param reason           why the technician rejected
 * @param rejectedAt       when the request was rejected
 */
public record ServiceRequestRejectedEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    String reason,
    Instant rejectedAt) {

  /**
   * Extracts the event fields from a request that has just been rejected.
   *
   * <p>The rejecting technician is passed explicitly because the request no longer holds it.</p>
   *
   * @param request               the rejected request
   * @param rejectingTechnicianId the technician who rejected
   * @param reason                why the technician rejected
   * @param rejectedAt            when it was rejected
   * @return the populated event
   */
  public static ServiceRequestRejectedEvent from(ServiceRequest request, TechnicianProfileId rejectingTechnicianId,
                                                 String reason, Instant rejectedAt) {
    return new ServiceRequestRejectedEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        rejectingTechnicianId.technicianProfileId(),
        reason,
        rejectedAt);
  }
}
