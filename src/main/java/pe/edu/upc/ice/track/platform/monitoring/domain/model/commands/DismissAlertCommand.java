package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to dismiss an alert as not actionable, without asserting the underlying
 * condition was resolved.
 *
 * <p>Owner-only: the command service applies it only when the caller owns the unit the
 * alert was raised on, and otherwise answers as if the alert did not exist.</p>
 *
 * @param alertId  identifier of the alert to dismiss
 * @param username username of the authenticated caller; required
 */
public record DismissAlertCommand(Long alertId, String username) {
}
