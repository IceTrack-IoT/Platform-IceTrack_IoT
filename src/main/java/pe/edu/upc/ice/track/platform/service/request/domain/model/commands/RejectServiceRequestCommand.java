package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

/**
 * Command to reject a service request assigned to the caller.
 *
 * @param serviceRequestId identifier of the request; required
 * @param username         username of the authenticated caller, resolved by the command service to
 *                         the technician the request must be assigned to; required
 * @param reason           why the technician rejects; required
 */
public record RejectServiceRequestCommand(
    Long serviceRequestId,
    String username,
    String reason) {
}
