package pe.edu.upc.ice.track.platform.service.request.domain.model.queries;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.CallerRole;

/**
 * Query to fetch the review of a service request.
 *
 * <p>Open to the owner who wrote the review and to the technician it is about.</p>
 *
 * @param serviceRequestId identifier of the reviewed request; required
 * @param username         username of the authenticated caller; required
 * @param callerRole       the role the caller acts in; {@code null} is treated as
 *                         {@link CallerRole#OWNER}
 */
public record GetReviewByServiceRequestIdQuery(Long serviceRequestId, String username, CallerRole callerRole) {
}
