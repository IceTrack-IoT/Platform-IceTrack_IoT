package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to list every equipment unit installed at a site.
 *
 * @param siteId   identifier of the site; required
 * @param username username of the authenticated caller, resolved by the query service to the owner
 *                 the site must belong to; required
 */
public record GetEquipmentBySiteQuery(Long siteId, String username) {
}