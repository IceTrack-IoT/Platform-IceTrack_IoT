package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

/**
 * Query to fetch the alert policy applicable to an equipment, on behalf of an authenticated caller.
 *
 * <p>Resolution falls back to the platform-wide default policy when no equipment-specific policy
 * is active. Answered as absent when the caller may not read the unit.</p>
 *
 * @param equipmentId identifier of the equipment
 * @param username    username of the authenticated caller; required
 * @param callerRole  the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record GetAlertPolicyByEquipmentQuery(Long equipmentId, String username, CallerRole callerRole) {
}
