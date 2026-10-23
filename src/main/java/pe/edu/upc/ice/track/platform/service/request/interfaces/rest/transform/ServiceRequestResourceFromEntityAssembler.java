package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.PagedServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServicePriorityResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceStatusResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceTypeResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.StatusTransitionResource;

import java.time.Instant;

/**
 * Assembler to convert a {@link ServiceRequest} aggregate to a {@link ServiceRequestResource}.
 */
public class ServiceRequestResourceFromEntityAssembler {

  /**
   * Converts a {@link ServiceRequest} aggregate to a {@link ServiceRequestResource}.
   *
   * @param entity the {@link ServiceRequest} aggregate to convert
   * @return the {@link ServiceRequestResource} resource
   */
  public static ServiceRequestResource toResourceFromEntity(ServiceRequest entity) {
    var serviceRequestId = entity.getId().serviceRequestId();
    return new ServiceRequestResource(
        serviceRequestId,
        entity.getOwnerId().ownerId(),
        entity.getEquipmentId().equipmentId(),
        entity.getSiteId().siteId(),
        entity.getTechnicianId() == null ? null : entity.getTechnicianId().technicianProfileId(),
        ServiceTypeResource.fromDomain(entity.getType()),
        ServicePriorityResource.fromDomain(entity.getPriority()),
        entity.getDescription(),
        ServiceStatusResource.fromDomain(entity.getStatus()),
        toIsoString(entity.getCreatedAt()),
        toIsoString(entity.getAssignedAt()),
        toIsoString(entity.getStartedAt()),
        toIsoString(entity.getCompletedAt()),
        toIsoString(entity.getCanceledAt()),
        entity.getCancelReason(),
        entity.getHistory().stream()
            .map(ServiceRequestResourceFromEntityAssembler::toResourceFromTransition)
            .toList(),
        entity.getIntervention() == null
            ? null
            : InterventionResourceFromEntityAssembler.toResourceFromIntervention(serviceRequestId, entity.getIntervention()));
  }

  /**
   * Converts a {@link ServiceRequestPage} into its outbound paged resource.
   *
   * @param page the page to convert
   * @return the {@link PagedServiceRequestResource} resource
   */
  public static PagedServiceRequestResource toPagedResourceFromPage(ServiceRequestPage page) {
    return new PagedServiceRequestResource(
        page.content().stream().map(ServiceRequestResourceFromEntityAssembler::toResourceFromEntity).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  private static StatusTransitionResource toResourceFromTransition(StatusTransition transition) {
    return new StatusTransitionResource(
        ServiceStatusResource.fromDomain(transition.getFromStatus()),
        ServiceStatusResource.fromDomain(transition.getToStatus()),
        transition.getActorId(),
        toIsoString(transition.getOccurredAt()),
        transition.getReason(),
        transition.getTechnicianId() == null ? null : transition.getTechnicianId().technicianProfileId());
  }

  private static String toIsoString(Instant value) {
    return value == null ? null : value.toString();
  }
}
