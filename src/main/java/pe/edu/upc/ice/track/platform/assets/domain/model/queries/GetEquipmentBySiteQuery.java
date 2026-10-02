package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to list every equipment unit installed at a site.
 *
 * @param siteId  identifier of the site; required
 * @param ownerId identifier of the owner requesting the list; required
 */
public record GetEquipmentBySiteQuery(Long siteId, Long ownerId) {
}