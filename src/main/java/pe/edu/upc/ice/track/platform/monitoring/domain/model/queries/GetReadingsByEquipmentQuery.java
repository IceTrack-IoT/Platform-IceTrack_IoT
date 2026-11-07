package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.time.LocalDateTime;

/**
 * Query to fetch the reading history of an equipment within a date range, on behalf of an
 * authenticated caller.
 *
 * <p>Answered as absent when the caller may not read the unit: the owner of the unit, or a
 * technician actively assigned to it.</p>
 *
 * @param equipmentId identifier of the equipment
 * @param from        inclusive start of the range
 * @param to          inclusive end of the range
 * @param username    username of the authenticated caller; required
 * @param callerRole  the role the caller acts in; {@code null} is treated as {@link CallerRole#OWNER}
 */
public record GetReadingsByEquipmentQuery(
    Long equipmentId, LocalDateTime from, LocalDateTime to, String username, CallerRole callerRole) {
}
