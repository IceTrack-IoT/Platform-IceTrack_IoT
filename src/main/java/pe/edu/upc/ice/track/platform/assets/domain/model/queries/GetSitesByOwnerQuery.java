package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to list every site belonging to an owner.
 *
 * @param ownerId identifier of the owner whose sites are listed; required
 */
public record GetSitesByOwnerQuery(Long ownerId) {
}