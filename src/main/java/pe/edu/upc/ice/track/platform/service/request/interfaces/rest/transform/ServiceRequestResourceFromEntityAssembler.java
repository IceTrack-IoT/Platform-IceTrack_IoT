package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.MaintenanceWindowLink;
import pe.edu.upc.ice.track.platform.service.request.domain.model.entities.StatusTransition;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.AcceptedServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestDisplayDetails;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestPage;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.IssuedEdgeGrantResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.MaintenanceWindowLinkResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.PagedServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.PagedServiceRequestSummaryResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServicePriorityResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceRequestResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceRequestSummaryResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceStatusResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.ServiceTypeResource;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.StatusTransitionResource;

import java.time.Instant;

/**
 * Assembler to convert a {@link ServiceRequest} aggregate to a {@link ServiceRequestResource} or,
 * for a light listing, a {@link ServiceRequestSummaryResource}.
 *
 * <p>The unit, site and technician names come from a {@link ServiceRequestDisplayDetails} resolved
 * beforehand for every request being converted, so converting a page reads no other context.</p>
 */
public class ServiceRequestResourceFromEntityAssembler {

  /**
   * Converts a {@link ServiceRequest} aggregate to a {@link ServiceRequestResource}.
   *
   * @param entity  the {@link ServiceRequest} aggregate to convert
   * @param details the names resolved for it; {@link ServiceRequestDisplayDetails#NONE} leaves them
   *                {@code null}
   * @return the {@link ServiceRequestResource} resource
   */
  public static ServiceRequestResource toResourceFromEntity(ServiceRequest entity, ServiceRequestDisplayDetails details) {
    return toResource(entity, details, null);
  }

  /**
   * Converts an accepted request into a {@link ServiceRequestResource} carrying the edge grant
   * issued with it.
   *
   * @param accepted the accepted request and its grant
   * @param details  the names resolved for the request
   * @return the {@link ServiceRequestResource} resource, with {@code edge_grant} set
   */
  public static ServiceRequestResource toResourceFromAccepted(
      AcceptedServiceRequest accepted, ServiceRequestDisplayDetails details) {
    return toResource(accepted.serviceRequest(), details,
        IssuedEdgeGrantResourceFromEntityAssembler.toResourceFromEntity(accepted.edgeGrant()));
  }

  /**
   * Converts a {@link ServiceRequest} aggregate to a {@link ServiceRequestSummaryResource}, without
   * its history, intervention details or maintenance window links.
   *
   * @param entity  the aggregate to convert
   * @param details the names and reviews resolved for it
   * @return the summary resource
   */
  public static ServiceRequestSummaryResource toSummaryResourceFromEntity(
      ServiceRequest entity, ServiceRequestDisplayDetails details) {
    var resolved = details == null ? ServiceRequestDisplayDetails.NONE : details;
    return new ServiceRequestSummaryResource(
        entity.getId().serviceRequestId(),
        entity.getEquipmentId().equipmentId(),
        entity.getSiteId().siteId(),
        entity.getTechnicianId() == null ? null : entity.getTechnicianId().technicianProfileId(),
        ServiceTypeResource.fromDomain(entity.getType()),
        ServicePriorityResource.fromDomain(entity.getPriority()),
        ServiceStatusResource.fromDomain(entity.getStatus()),
        toIsoString(entity.getCreatedAt()),
        toIsoString(entity.getAssignedAt()),
        toIsoString(entity.getStartedAt()),
        toIsoString(entity.getCompletedAt()),
        toIsoString(entity.getCanceledAt()),
        resolved.equipmentNameOf(entity),
        resolved.siteNameOf(entity),
        resolved.technicianNameOf(entity),
        entity.getIntervention() != null,
        resolved.isReviewed(entity),
        entity.getSourceAlertId());
  }

  private static ServiceRequestResource toResource(
      ServiceRequest entity, ServiceRequestDisplayDetails details, IssuedEdgeGrantResource edgeGrant) {
    var resolved = details == null ? ServiceRequestDisplayDetails.NONE : details;
    var serviceRequestId = entity.getId().serviceRequestId();
    return new ServiceRequestResource(
        serviceRequestId,
        entity.getOwnerId().ownerId(),
        entity.getEquipmentId().equipmentId(),
        entity.getSiteId().siteId(),
        entity.getTechnicianId() == null ? null : entity.getTechnicianId().technicianProfileId(),
        resolved.equipmentNameOf(entity),
        resolved.siteNameOf(entity),
        resolved.technicianNameOf(entity),
        ServiceTypeResource.fromDomain(entity.getType()),
        ServicePriorityResource.fromDomain(entity.getPriority()),
        entity.getDescription(),
        entity.getSourceAlertId(),
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
            : InterventionResourceFromEntityAssembler.toResourceFromIntervention(serviceRequestId, entity.getIntervention()),
        entity.getMaintenanceWindows().stream()
            .map(ServiceRequestResourceFromEntityAssembler::toResourceFromLink)
            .toList(),
        edgeGrant);
  }

  private static MaintenanceWindowLinkResource toResourceFromLink(MaintenanceWindowLink link) {
    return new MaintenanceWindowLinkResource(
        link.getWindowId(),
        link.getOrigin().name(),
        link.getTechnicianProfileId(),
        toIsoString(link.getStartedAt()),
        toIsoString(link.getEndedAt()),
        link.getClosureReason());
  }

  /**
   * Converts a {@link ServiceRequestPage} into its outbound paged resource of full requests.
   *
   * @param page    the page to convert
   * @param details the names resolved for the requests of the page
   * @return the {@link PagedServiceRequestResource} resource
   */
  public static PagedServiceRequestResource toPagedResourceFromPage(
      ServiceRequestPage page, ServiceRequestDisplayDetails details) {
    return new PagedServiceRequestResource(
        page.content().stream().map(request -> toResourceFromEntity(request, details)).toList(),
        page.page(),
        page.size(),
        page.totalElements(),
        page.totalPages());
  }

  /**
   * Converts a {@link ServiceRequestPage} into its outbound paged resource of summaries.
   *
   * @param page    the page to convert
   * @param details the names and reviews resolved for the requests of the page
   * @return the {@link PagedServiceRequestSummaryResource} resource
   */
  public static PagedServiceRequestSummaryResource toPagedSummaryResourceFromPage(
      ServiceRequestPage page, ServiceRequestDisplayDetails details) {
    return new PagedServiceRequestSummaryResource(
        page.content().stream().map(request -> toSummaryResourceFromEntity(request, details)).toList(),
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
