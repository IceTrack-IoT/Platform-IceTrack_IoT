package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to issue a stream ticket: the short-lived, single-use credential a browser presents to
 * open the telemetry stream, since {@code EventSource} cannot send an {@code Authorization} header.
 *
 * <p>The ticket is bound to the owner profile of the authenticated caller, resolved by the command
 * service; it is never taken from the request.</p>
 *
 * @param username username of the authenticated caller
 */
public record IssueStreamTicketCommand(String username) {
}
