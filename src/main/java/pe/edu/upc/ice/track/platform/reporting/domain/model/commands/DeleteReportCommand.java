package pe.edu.upc.ice.track.platform.reporting.domain.model.commands;

/**
 * Command to delete one of the caller's finished reports, together with its stored file.
 *
 * @param reportId identifier of the report; required
 * @param username username of the authenticated caller, who must be the requester; required
 */
public record DeleteReportCommand(Long reportId, String username) {
}
