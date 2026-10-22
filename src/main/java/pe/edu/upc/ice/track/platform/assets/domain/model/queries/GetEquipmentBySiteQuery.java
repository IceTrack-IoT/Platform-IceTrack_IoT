package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;

/**
 * Query to list every equipment unit installed at a site.
 *
 * @param siteId     identifier of the site; required
 * @param username   username of the authenticated caller, resolved by the query service to the
 *                   owner the site must belong to when the caller acts as an owner; required
 * @param callerRole the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record GetEquipmentBySiteQuery(Long siteId, String username, CallerRole callerRole) {
}