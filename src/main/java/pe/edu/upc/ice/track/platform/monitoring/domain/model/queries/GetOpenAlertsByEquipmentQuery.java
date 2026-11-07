package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

/**
 * Query to list the currently OPEN or ACKNOWLEDGED alerts for a given equipment, on behalf of an
 * authenticated caller.
 *
 * <p>Answered with an empty list when the caller may not read the unit.</p>
 *
 * @param equipmentId identifier of the equipment
 * @param username    username of the authenticated caller; required
 * @param callerRole  the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record GetOpenAlertsByEquipmentQuery(Long equipmentId, String username, CallerRole callerRole) {
}
