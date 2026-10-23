package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.time.Instant;

/**
 * Domain event published when the assigned technician accepts a service request.
 *
 * @param serviceRequestId the request
 * @param ownerId          the owner of the request, who is waiting for this answer
 * @param technicianId     the technician who accepted
 * @param acceptedAt       when the request was accepted
 */
public record ServiceRequestAcceptedEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    Instant acceptedAt) {

  /**
   * Extracts the event fields from a request that has just been accepted.
   *
   * @param request    the accepted request
   * @param acceptedAt when it was accepted
   * @return the populated event
   */
  public static ServiceRequestAcceptedEvent from(ServiceRequest request, Instant acceptedAt) {
    return new ServiceRequestAcceptedEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        request.getTechnicianId().technicianProfileId(),
        acceptedAt);
  }
}
