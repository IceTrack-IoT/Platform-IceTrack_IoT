package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

/**
 * Query to list the equipment units of an owner, with optional filters and optional paging.
 *
 * <p>Backs the dashboard listing, which is read far more often than it is written, so every
 * filter is resolved in the database rather than as a post-filter. A {@code null} filter means
 * "do not restrict on this dimension", and a {@code null} {@code page} or {@code size} means
 * "return everything in one page", which keeps the basic listing contract unchanged for clients
 * that do not paginate.</p>
 *
 * <p>The free-text {@code search} is normalized here, once, so no layer below has to guess what an
 * empty box means: surrounding whitespace is stripped and a blank value becomes {@code null}, which
 * is what keeps a cleared search box from turning into a match-everything {@code LIKE '%%'}.</p>
 *
 * @param username      username of the authenticated caller, resolved by the query service to the
 *                      owner whose units are listed; required
 * @param search        case-insensitive text matched against the unit name or uid, may be
 *                      {@code null}
 * @param siteId        restrict to a single site, may be {@code null}
 * @param status        restrict to a single lifecycle status, may be {@code null}
 * @param equipmentType restrict to a single kind of refrigeration unit, may be {@code null}
 * @param page          zero-based page index, may be {@code null} for an unpaged listing
 * @param size          page size, at most {@link #MAX_PAGE_SIZE}; may be {@code null} for an
 *                      unpaged listing
 */
public record GetEquipmentByOwnerQuery(
    String username,
    String search,
    Long siteId,
    StatusEquipment status,
    EquipmentType equipmentType,
    Integer page,
    Integer size) {

  /**
   * Largest page a client may ask for. A paged request is how a client walks a large collection,
   * so letting it ask for an arbitrarily large page would reopen the unbounded read paging exists
   * to prevent.
   */
  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Normalizes the search text and validates the paging values that were supplied.
   *
   * @throws IllegalArgumentException when the page index is negative or the page size is outside
   *                                  {@code 1..MAX_PAGE_SIZE}
   */
  public GetEquipmentByOwnerQuery {
    search = search == null || search.isBlank() ? null : search.strip();
    if (page != null && page < 0) {
      throw new IllegalArgumentException("Page index must not be negative");
    }
    if (size != null && (size < 1 || size > MAX_PAGE_SIZE)) {
      throw new IllegalArgumentException("Page size must be between 1 and %d".formatted(MAX_PAGE_SIZE));
    }
  }

  /**
   * Convenience factory for the unfiltered, unpaged listing.
   *
   * @param username username of the authenticated caller whose units are listed
   * @return a query with no filter and no paging applied
   */
  public static GetEquipmentByOwnerQuery all(String username) {
    return new GetEquipmentByOwnerQuery(username, null, null, null, null, null, null);
  }

  /**
   * Tells whether this query asks for a single slice rather than the whole collection.
   *
   * @return {@code true} when both a page index and a page size were supplied
   */
  public boolean isPaged() {
    return page != null && size != null;
  }
}
