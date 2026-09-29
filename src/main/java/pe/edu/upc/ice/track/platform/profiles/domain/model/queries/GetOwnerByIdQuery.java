package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

/**
 * Query to retrieve an owner by its identifier.
 *
 * @param ownerId the owner identifier
 */
public record GetOwnerByIdQuery(Long ownerId) {
}
