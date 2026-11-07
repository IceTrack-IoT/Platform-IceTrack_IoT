package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to acknowledge an OPEN alert.
 *
 * <p>The command service applies it only when the caller owns the unit the
 * alert was raised on, and otherwise answers as if the alert did not exist.</p>
 *
 * @param alertId  identifier of the alert to acknowledge
 * @param username username of the authenticated caller
 */
public record AcknowledgeAlertCommand(Long alertId, String username) {
}
