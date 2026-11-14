package pe.edu.upc.ice.track.platform.device.domain.model.queries;

import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

import java.util.Set;

/**
 * Query for the boards linked - paired or revoked - to the units of the caller, with optional
 * filters and optional paging.
 *
 * <p>Without a filter it lists the whole inventory of the owner in one read. An unpaired board is
 * linked to no unit and therefore belongs to no owner, so it is never part of the answer, whatever
 * the status or connectivity filter says.</p>
 *
 * <p>The listing is paged only when both {@code page} and {@code size} are supplied; otherwise every
 * matching board is answered at once. A page size above {@link #MAX_PAGE_SIZE} is capped to it
 * rather than refused.</p>
 *
 * @param username     username of the authenticated caller, resolved to the owner whose boards are
 *                     listed; required
 * @param equipmentId  restrict to the board of a single unit of the caller, may be {@code null}
 * @param siteId       restrict to the boards of the units of a single site of the caller, may be
 *                     {@code null}
 * @param statuses     restrict to these lifecycle statuses; empty does not restrict
 * @param search       case-insensitive text matched against the board uid or model, may be
 *                     {@code null}
 * @param page         zero-based page index, may be {@code null} for an unpaged listing
 * @param size         page size, may be {@code null} for an unpaged listing
 * @param connectivity restrict to these connectivity states, evaluated at read time; empty does not
 *                     restrict
 */
public record GetDevicesQuery(
    String username,
    Long equipmentId,
    Long siteId,
    Set<DeviceStatus> statuses,
    String search,
    Integer page,
    Integer size,
    Set<DeviceConnectivityState> connectivity) {

  /** Largest page a client is served. */
  public static final int MAX_PAGE_SIZE = 100;

  /** Longest search text accepted, the width of the columns it is matched against. */
  public static final int MAX_SEARCH_LENGTH = 64;

  /**
   * Normalizes the filters and validates the paging values that were supplied.
   *
   * @throws IllegalArgumentException when the search text is too long, the page index is negative or
   *                                  the page size is below one
   */
  public GetDevicesQuery {
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    connectivity = connectivity == null ? Set.of() : Set.copyOf(connectivity);
    search = search == null || search.isBlank() ? null : search.strip();
    if (search != null && search.length() > MAX_SEARCH_LENGTH) {
      throw new IllegalArgumentException(
          "Parameter 'search' must not exceed %d characters".formatted(MAX_SEARCH_LENGTH));
    }
    if (page != null && page < 0) {
      throw new IllegalArgumentException("Parameter 'page' must not be negative");
    }
    if (size != null && size < 1) {
      throw new IllegalArgumentException("Parameter 'size' must be at least 1");
    }
    if (size != null && size > MAX_PAGE_SIZE) {
      size = MAX_PAGE_SIZE;
    }
  }

  /**
   * Creates the query without a connectivity filter.
   */
  public GetDevicesQuery(String username, Long equipmentId, Long siteId, Set<DeviceStatus> statuses,
                         String search, Integer page, Integer size) {
    this(username, equipmentId, siteId, statuses, search, page, size, Set.of());
  }

  /**
   * Tells whether this query asks for a single slice rather than the whole collection.
   *
   * @return {@code true} when both a page index and a page size were supplied
   */
  public boolean isPaged() {
    return page != null && size != null;
  }

  /**
   * Tells whether the boards must be narrowed by their connectivity, which is only known once they
   * are evaluated.
   *
   * @return {@code true} when a connectivity filter was supplied
   */
  public boolean filtersByConnectivity() {
    return !connectivity.isEmpty();
  }
}
