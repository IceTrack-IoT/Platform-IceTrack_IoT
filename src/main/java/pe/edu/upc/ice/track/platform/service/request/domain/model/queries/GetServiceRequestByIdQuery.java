package pe.edu.upc.ice.track.platform.service.request.domain.model.queries;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.CallerRole;

/**
 * Query to fetch a single service request, with its status history.
 *
 * @param serviceRequestId identifier of the request; required
 * @param username         username of the authenticated caller, resolved by the query service to
 *                         the owner or the technician it acts as; required
 * @param callerRole       the role the caller acts in; {@code null} is treated as
 *                         {@link CallerRole#OWNER}
 */
public record GetServiceRequestByIdQuery(Long serviceRequestId, String username, CallerRole callerRole) {
}
