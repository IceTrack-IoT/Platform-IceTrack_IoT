package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

/**
 * Query to fetch a single alert by identifier, on behalf of an authenticated caller.
 *
 * <p>The alert is returned only when the caller may read its unit: the owner of the unit, or a
 * technician actively assigned to it. Anything else is answered as absent.</p>
 *
 * @param alertId    identifier of the alert
 * @param username   username of the authenticated caller; required
 * @param callerRole the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record GetAlertByIdQuery(Long alertId, String username, CallerRole callerRole) {
}
