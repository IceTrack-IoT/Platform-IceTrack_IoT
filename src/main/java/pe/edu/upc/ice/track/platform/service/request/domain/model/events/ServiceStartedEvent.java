package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.time.Instant;

/**
 * Domain event published when the assigned technician starts working on a service request.
 *
 * @param serviceRequestId the request
 * @param ownerId          the owner of the request
 * @param technicianId     the technician who started
 * @param equipmentId      the unit being serviced
 * @param startedAt        when the work started, as reported by the technician's device
 */
public record ServiceStartedEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    Long equipmentId,
    Instant startedAt) {

  /**
   * Extracts the event fields from a request that has just been started.
   *
   * @param request the started request
   * @return the populated event
   */
  public static ServiceStartedEvent from(ServiceRequest request) {
    return new ServiceStartedEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        request.getTechnicianId().technicianProfileId(),
        request.getEquipmentId().equipmentId(),
        request.getStartedAt());
  }
}
