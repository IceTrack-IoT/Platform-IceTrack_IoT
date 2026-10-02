package pe.edu.upc.ice.track.platform.monitoring.interfaces.events;

/**
 * Integration event published to the platform-wide Spring event bus when a new alert is raised.
 *
 * <p>This is the published language of the {@code monitoring} bounded context for this fact.
 * Other bounded contexts — chiefly Notifications — must subscribe to this event, never to the
 * internal {@code monitoring.domain.model.events.AlertRaisedEvent}.</p>
 *
 * @param alertId         identity of the raised alert
 * @param equipmentId     identifier of the equipment the alert concerns
 * @param type            name of the {@code AlertType} raised, as plain text
 * @param severity        name of the {@code AlertSeverity} assigned, as plain text
 * @param peakTemperature peak temperature observed, may be {@code null}
 */
public record AlertRaisedIntegrationEvent(
    Long alertId,
    Long equipmentId,
    String type,
    String severity,
    Double peakTemperature) {
}
