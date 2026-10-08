package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.EquipmentType;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.StatusEquipment;

/**
 * Query to list the equipment units of an owner, with optional filters and optional paging.
 *
 * <p>Backs the dashboard listing, which is read far more often than it is written, so every
 * filter is an optional indexable column rather than a post-filter. A {@code null} filter means
 * "do not restrict on this dimension", and a {@code null} {@code page} or {@code size} means
 * "return everything in one page", which keeps the basic listing contract unchanged for clients
 * that do not paginate.</p>
 *
 * @param ownerId       identifier of the owner whose units are listed; required
 * @param siteId        restrict to a single site, may be {@code null}
 * @param status        restrict to a single operational status, may be {@code null}
 * @param equipmentType restrict to a single kind of refrigeration unit, may be {@code null}
 * @param page          zero-based page index, may be {@code null} for an unpaged listing
 * @param size          page size, may be {@code null} for an unpaged listing
 */
public record GetEquipmentByOwnerQuery(
    Long ownerId,
    Long siteId,
    StatusEquipment status,
    EquipmentType equipmentType,
    Integer page,
    Integer size) {

  /**
   * Convenience factory for the unfiltered, unpaged listing.
   *
   * @param ownerId identifier of the owner whose units are listed
   * @return a query with no filter and no paging applied
   */
  public static GetEquipmentByOwnerQuery all(Long ownerId) {
    return new GetEquipmentByOwnerQuery(ownerId, null, null, null, null, null);
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