package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;

/**
 * Query to fetch a single site by its identifier.
 *
 * <p>Carries the requesting caller as well, so that the query service can resolve its owner, apply
 * the ownership rule and answer "not yours" without the caller having to fetch the site first to
 * find out. A caller acting as a technician is not scoped by ownership.</p>
 *
 * @param siteId     identifier of the site; required
 * @param username   username of the authenticated caller requesting the site; required
 * @param callerRole the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record GetSiteByIdQuery(Long siteId, String username, CallerRole callerRole) {
}