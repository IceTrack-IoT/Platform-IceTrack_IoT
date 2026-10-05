package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to fetch a single equipment unit by its identifier.
 *
 * <p>{@code ownerId} is nullable on purpose: this query is also the one the ACL facade uses to
 * answer other bounded contexts, which legitimately ask about a unit without being its owner.</p>
 *
 * @param equipmentId identifier of the unit; required
 * @param ownerId     identifier of the owner requesting the unit, may be {@code null}
 */
public record GetEquipmentByIdQuery(Long equipmentId, Long ownerId) {
}