package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

/**
 * Domain event published when a new {@code Alert} is raised.
 *
 * <p>Other bounded contexts must not subscribe to this event directly; it is translated into
 * the published language of the {@code monitoring} context by
 * {@code monitoring.application.internal.eventhandlers.AlertRaisedEventHandler}.</p>
 *
 * @param alertId         identity assigned to the newly raised alert
 * @param equipmentId     identifier of the equipment the alert concerns
 * @param type            the kind of anomalous condition raised
 * @param severity        how urgently the alert should be surfaced
 * @param peakTemperature the peak temperature observed, may be {@code null} for a
 *                        {@code DEVICE_OFFLINE} alert
 */
public record AlertRaisedEvent(
    Long alertId,
    Long equipmentId,
    AlertType type,
    AlertSeverity severity,
    Double peakTemperature) {
}
