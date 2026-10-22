package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

/**
 * Query to fetch a single equipment unit by its identifier.
 *
 * @param equipmentId identifier of the unit; required
 * @param username    username of the authenticated caller, resolved by the query service to the
 *                    owner the unit must belong to; required
 */
public record GetEquipmentByIdQuery(Long equipmentId, String username) {
}