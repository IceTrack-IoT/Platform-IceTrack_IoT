package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

/**
 * Command to cancel one of the caller's service requests.
 *
 * @param serviceRequestId identifier of the request; required
 * @param username         username of the authenticated caller, resolved by the command service to
 *                         the owner the request must belong to; required
 * @param reason           why the owner cancels; required
 */
public record CancelServiceRequestCommand(
    Long serviceRequestId,
    String username,
    String reason) {
}
