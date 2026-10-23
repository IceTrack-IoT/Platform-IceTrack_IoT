package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;

import java.time.Instant;

/**
 * Domain event published when an owner creates a service request.
 *
 * @param serviceRequestId the identity assigned to the new request
 * @param ownerId          the owner who created it
 * @param equipmentId      the unit to service
 * @param siteId           the site the unit is installed at
 * @param type             the kind of work
 * @param priority         the urgency
 * @param createdAt        when the request was created
 */
public record ServiceRequestCreatedEvent(
    Long serviceRequestId,
    Long ownerId,
    Long equipmentId,
    Long siteId,
    ServiceType type,
    ServicePriority priority,
    Instant createdAt) {

  /**
   * Extracts the event fields from a saved {@link ServiceRequest}.
   *
   * @param request the saved request (must already carry its identity)
   * @return the populated event
   */
  public static ServiceRequestCreatedEvent from(ServiceRequest request) {
    return new ServiceRequestCreatedEvent(
        request.getId().serviceRequestId(),
        request.getOwnerId().ownerId(),
        request.getEquipmentId().equipmentId(),
        request.getSiteId().siteId(),
        request.getType(),
        request.getPriority(),
        request.getCreatedAt());
  }
}
