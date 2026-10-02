package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to acknowledge an OPEN alert.
 *
 * @param alertId identifier of the alert to acknowledge
 */
public record AcknowledgeAlertCommand(Long alertId) {
}
