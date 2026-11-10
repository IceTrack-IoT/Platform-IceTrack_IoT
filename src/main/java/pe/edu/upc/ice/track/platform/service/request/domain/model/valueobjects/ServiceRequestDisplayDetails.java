package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;

import java.util.Map;
import java.util.Set;

/**
 * What a set of service requests is shown with but does not hold itself: the names of their units,
 * sites and technicians, which live in other contexts, and which of them were reviewed.
 *
 * <p>A read model resolved for a whole page at once, so a listing reads each other context once
 * rather than once per request. Every lookup answers {@code null} (or {@code false}) when the value
 * is unknown, such as a unit deleted since the request was opened.</p>
 *
 * @param equipmentNames            unit names, by unit identifier
 * @param siteNames                 site names, by site identifier
 * @param technicianNames           technician full names, by technician profile identifier
 * @param reviewedServiceRequestIds identifiers of the requests that have a review
 */
public record ServiceRequestDisplayDetails(
    Map<Long, String> equipmentNames,
    Map<Long, String> siteNames,
    Map<Long, String> technicianNames,
    Set<Long> reviewedServiceRequestIds) {

  /** Details that know nothing. */
  public static final ServiceRequestDisplayDetails NONE =
      new ServiceRequestDisplayDetails(Map.of(), Map.of(), Map.of(), Set.of());

  /** Normalizes missing collections to empty, immutable ones. */
  public ServiceRequestDisplayDetails {
    equipmentNames = equipmentNames == null ? Map.of() : Map.copyOf(equipmentNames);
    siteNames = siteNames == null ? Map.of() : Map.copyOf(siteNames);
    technicianNames = technicianNames == null ? Map.of() : Map.copyOf(technicianNames);
    reviewedServiceRequestIds = reviewedServiceRequestIds == null ? Set.of() : Set.copyOf(reviewedServiceRequestIds);
  }

  /**
   * @param request the request
   * @return the name of its unit, or {@code null} when unknown
   */
  public String equipmentNameOf(ServiceRequest request) {
    return equipmentNames.get(request.getEquipmentId().equipmentId());
  }

  /**
   * @param request the request
   * @return the name of the site it was raised for, or {@code null} when unknown
   */
  public String siteNameOf(ServiceRequest request) {
    return siteNames.get(request.getSiteId().siteId());
  }

  /**
   * @param request the request
   * @return the full name of its assigned technician, or {@code null} while none is assigned or
   *         when unknown
   */
  public String technicianNameOf(ServiceRequest request) {
    return request.getTechnicianId() == null
        ? null
        : technicianNames.get(request.getTechnicianId().technicianProfileId());
  }

  /**
   * @param request the request
   * @return {@code true} when the request has a review
   */
  public boolean isReviewed(ServiceRequest request) {
    return request.getId() != null && reviewedServiceRequestIds.contains(request.getId().serviceRequestId());
  }
}
