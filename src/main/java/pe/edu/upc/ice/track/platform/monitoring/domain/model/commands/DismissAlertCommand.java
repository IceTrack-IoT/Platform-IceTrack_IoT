package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

/**
 * Command to dismiss an alert as not actionable, without asserting the underlying
 * condition was resolved.
 *
 * @param alertId identifier of the alert to dismiss
 */
public record DismissAlertCommand(Long alertId) {
}
