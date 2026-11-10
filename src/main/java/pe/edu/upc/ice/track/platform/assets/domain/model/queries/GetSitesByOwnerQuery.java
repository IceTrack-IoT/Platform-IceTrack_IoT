package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to list every site belonging to an owner.
 *
 * @param username username of the authenticated caller, resolved by the query service to the owner
 *                 whose sites are listed; required
 */
public record GetSitesByOwnerQuery(String username) {
}