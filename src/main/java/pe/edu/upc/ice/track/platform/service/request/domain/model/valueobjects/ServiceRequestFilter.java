package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

import java.time.Instant;
import java.util.Set;

/**
 * Optional restrictions on which of the caller's service requests a listing returns.
 *
 * <p>The scope - the owner who created the requests, or the technician they are assigned to - is
 * never part of the filter: it is resolved from the caller and applied first, so every restriction
 * here only narrows the caller's own requests. A unit, site or alert of another owner simply matches
 * nothing.</p>
 *
 * @param statuses      restrict to these statuses; {@code null} or empty does not restrict
 * @param equipmentId   restrict to the requests for one unit; {@code null} does not restrict
 * @param siteId        restrict to the requests raised for one site; {@code null} does not restrict
 * @param types         restrict to these kinds of work; {@code null} or empty does not restrict
 * @param priorities    restrict to these urgencies; {@code null} or empty does not restrict
 * @param createdFrom   inclusive lower bound of the creation instant; {@code null} does not restrict
 * @param createdTo     inclusive upper bound of the creation instant; {@code null} does not restrict
 * @param sourceAlertId restrict to the requests opened for one alert; {@code null} does not restrict
 */
public record ServiceRequestFilter(
    Set<ServiceStatus> statuses,
    Long equipmentId,
    Long siteId,
    Set<ServiceType> types,
    Set<ServicePriority> priorities,
    Instant createdFrom,
    Instant createdTo,
    Long sourceAlertId) {

  /** A filter that restricts nothing. */
  public static final ServiceRequestFilter NONE =
      new ServiceRequestFilter(Set.of(), null, null, Set.of(), Set.of(), null, null, null);

  /**
   * Normalizes missing sets to empty, immutable ones and validates the creation range.
   *
   * @throws IllegalArgumentException when {@code createdFrom} is after {@code createdTo}
   */
  public ServiceRequestFilter {
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    types = types == null ? Set.of() : Set.copyOf(types);
    priorities = priorities == null ? Set.of() : Set.copyOf(priorities);
    if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
      throw new IllegalArgumentException("Parameter 'created_from' must not be after 'created_to'");
    }
  }
}
