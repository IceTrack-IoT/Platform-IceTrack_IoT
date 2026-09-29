package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to resolve an alert once the underlying condition has stopped applying.
 *
 * @param alertId identifier of the alert to resolve
 */
public record ResolveAlertCommand(Long alertId) {
}
