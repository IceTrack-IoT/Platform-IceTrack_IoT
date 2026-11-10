package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to resolve an alert once the underlying condition has stopped applying.
 *
 * <p>Owner-only: the command service applies it only when the caller owns the unit the
 * alert was raised on, and otherwise answers as if the alert did not exist. The caller is
 * recorded as the user who resolved the alert.</p>
 *
 * @param alertId  identifier of the alert to resolve
 * @param username username of the authenticated caller; required
 */
public record ResolveAlertCommand(Long alertId, String username) {
}
