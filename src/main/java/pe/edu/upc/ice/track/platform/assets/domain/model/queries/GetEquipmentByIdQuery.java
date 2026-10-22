package pe.edu.upc.ice.track.platform.assets.domain.model.queries;

import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.CallerRole;

/**
 * Query to fetch a single equipment unit by its identifier.
 *
 * @param equipmentId identifier of the unit; required
 * @param username    username of the authenticated caller, resolved by the query service to the
 *                    owner the unit must belong to when the caller acts as an owner; required
 * @param callerRole  the role the caller acts in; {@code null} is treated as
 *                    {@link CallerRole#OWNER}
 */
public record GetEquipmentByIdQuery(Long equipmentId, String username, CallerRole callerRole) {
}