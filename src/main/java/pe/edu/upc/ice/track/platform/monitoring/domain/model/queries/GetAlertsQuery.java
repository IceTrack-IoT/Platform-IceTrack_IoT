package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

import java.time.Instant;
import java.util.Set;

/**
 * Query for the alerts of every unit the caller owns, whatever their status, with optional filters.
 *
 * <p>Always paged, most recently opened first: the history of an owner's alerts only grows, so an
 * unpaged form would grow without bound with it. A page size above {@link #MAX_PAGE_SIZE} is capped
 * to it rather than refused.</p>
 *
 * @param username    username of the authenticated caller, resolved to the owner whose alerts are
 *                    listed; required
 * @param statuses    restrict to these lifecycle statuses; empty does not restrict
 * @param equipmentId restrict to a single unit of the caller, may be {@code null}
 * @param siteId      restrict to the units of a single site of the caller, may be {@code null}
 * @param types       restrict to these kinds of alert; empty does not restrict
 * @param severities  restrict to these severities; empty does not restrict
 * @param openedFrom  inclusive lower bound of the opening instant, may be {@code null}
 * @param openedTo    inclusive upper bound of the opening instant, may be {@code null}
 * @param page        zero-based page index; not negative
 * @param size        page size; at least one
 */
public record GetAlertsQuery(
    String username,
    Set<AlertStatus> statuses,
    Long equipmentId,
    Long siteId,
    Set<AlertType> types,
    Set<AlertSeverity> severities,
    Instant openedFrom,
    Instant openedTo,
    int page,
    int size) {

  /** Page index used when the caller names none. */
  public static final int DEFAULT_PAGE = 0;

  /** Page size used when the caller names none. */
  public static final int DEFAULT_PAGE_SIZE = 20;

  /** Largest page a client is served. */
  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Normalizes the filters and validates the paging values and the period.
   *
   * @throws IllegalArgumentException when the page index is negative, the page size is below one or
   *                                  the period is inverted
   */
  public GetAlertsQuery {
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    types = types == null ? Set.of() : Set.copyOf(types);
    severities = severities == null ? Set.of() : Set.copyOf(severities);
    if (page < 0) {
      throw new IllegalArgumentException("Parameter 'page' must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("Parameter 'size' must be at least 1");
    }
    if (size > MAX_PAGE_SIZE) {
      size = MAX_PAGE_SIZE;
    }
    if (openedFrom != null && openedTo != null && openedFrom.isAfter(openedTo)) {
      throw new IllegalArgumentException("Parameter 'opened_from' must not be after 'opened_to'");
    }
  }
}
