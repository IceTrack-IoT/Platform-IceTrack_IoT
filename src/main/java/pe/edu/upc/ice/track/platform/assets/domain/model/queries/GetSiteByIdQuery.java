package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to fetch a single site by its identifier.
 *
 * <p>Carries the requesting owner as well, so that the query service can apply the ownership rule
 * and answer "not yours" without the caller having to fetch the site first to find out.</p>
 *
 * @param siteId  identifier of the site; required
 * @param ownerId identifier of the owner requesting the site; required
 */
public record GetSiteByIdQuery(Long siteId, Long ownerId) {
}