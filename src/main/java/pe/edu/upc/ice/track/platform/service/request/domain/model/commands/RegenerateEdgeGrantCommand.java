package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

/**
 * Command issued by the assigned technician to replace a lost edge grant.
 *
 * <p>The previous grant of the request is revoked and a new one is issued; the new plaintext token
 * is returned once.</p>
 *
 * @param serviceRequestId the request whose grant is regenerated
 * @param username         username of the authenticated caller; required
 */
public record RegenerateEdgeGrantCommand(Long serviceRequestId, String username) {
}
