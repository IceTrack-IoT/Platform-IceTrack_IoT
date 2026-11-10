package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

/**
 * Command to assign a technician to one of the caller's service requests.
 *
 * @param serviceRequestId    identifier of the request; required
 * @param technicianProfileId identifier of the technician profile to assign; required and eligible
 *                            for assignment
 * @param username            username of the authenticated caller, resolved by the command service
 *                            to the owner the request must belong to; required
 */
public record AssignTechnicianCommand(
    Long serviceRequestId,
    Long technicianProfileId,
    String username) {
}
