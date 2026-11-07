package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.TechnicianProfileId;

import java.time.Instant;

/**
 * Domain event published when an owner assigns a technician to a service request, including a
 * reassignment that replaces the previous technician.
 *
 * @param serviceRequestId     the request
 * @param ownerId              the owner who assigned
 * @param technicianId         the technician now assigned
 * @param previousTechnicianId the technician replaced, {@code null} when there was none
 * @param equipmentId          the unit to service
 * @param siteId               the site the unit is installed at
 * @param type                 the kind of work
 * @param priority             the urgency
 * @param assignedAt           when the assignment happened
 */
public record TechnicianAssignedEvent(
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    Long previousTechnicianId,
    Long equipmentId,
    Long siteId,
    ServiceType type,
    ServicePriority priority,
    Instant assignedAt) {

  /**
   * Extracts the event fields from a request that has just been assigned.
   *
   * @param request              the request, already holding the new technician
   * @param previousTechnicianId the technician replaced, may be {@code null}
   * @return the populated event
   */
  public static TechnicianAssignedEvent from(ServiceRequest request, TechnicianProfileId previousTechnicianId) {
    return new TechnicianAssignedEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        request.getTechnicianId().technicianProfileId(),
        previousTechnicianId == null ? null : previousTechnicianId.technicianProfileId(),
        request.getEquipmentId().equipmentId(),
        request.getSiteId().siteId(),
        request.getType(),
        request.getPriority(),
        request.getAssignedAt());
  }
}
