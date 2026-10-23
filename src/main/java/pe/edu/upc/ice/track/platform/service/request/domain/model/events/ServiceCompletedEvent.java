package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event published when the assigned technician registers the intervention that completes a
 * service request.
 *
 * <p>Raised once per request: a replayed submission of the same intervention raises nothing.</p>
 *
 * @param serviceRequestId  the request
 * @param ownerId           the owner of the request, who may now review it
 * @param technicianId      the technician who completed it
 * @param equipmentId       the unit that was serviced
 * @param clientGeneratedId the identifier of the registered intervention
 * @param completedAt       when the work finished
 */
public record ServiceCompletedEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    Long equipmentId,
    UUID clientGeneratedId,
    Instant completedAt) {

  /**
   * Extracts the event fields from a request that has just been completed.
   *
   * @param request the completed request, already holding its intervention
   * @return the populated event
   */
  public static ServiceCompletedEvent from(ServiceRequest request) {
    return new ServiceCompletedEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        request.getTechnicianId().technicianProfileId(),
        request.getEquipmentId().equipmentId(),
        request.getIntervention().getClientGeneratedId(),
        request.getCompletedAt());
  }
}
