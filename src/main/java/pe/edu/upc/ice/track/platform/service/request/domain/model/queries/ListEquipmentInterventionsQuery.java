package pe.edu.upc.ice.track.platform.service.request.domain.model.queries;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.CallerRole;

/**
 * Query to list the past interventions performed on a unit, most recent first.
 *
 * <p>Open to the owner of the unit, and to a technician who is or was assigned to a request on it -
 * the one who is about to service it, or who already did.</p>
 *
 * @param equipmentId identifier of the unit; required
 * @param username    username of the authenticated caller; required
 * @param callerRole  the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record ListEquipmentInterventionsQuery(Long equipmentId, String username, CallerRole callerRole) {
}
