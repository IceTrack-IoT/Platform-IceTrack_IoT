package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to fetch a single site by its identifier.
 *
 * <p>Carries the requesting caller as well, so that the query service can resolve its owner, apply
 * the ownership rule and answer "not yours" without the caller having to fetch the site first to
 * find out.</p>
 *
 * @param siteId   identifier of the site; required
 * @param username username of the authenticated caller requesting the site; required
 */
public record GetSiteByIdQuery(Long siteId, String username) {
}