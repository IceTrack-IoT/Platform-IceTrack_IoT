package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

import java.time.Instant;

/**
 * Domain event registered every time an {@code Alert} enters a status: when it is raised
 * ({@code OPEN}) and on each later transition ({@code ACKNOWLEDGED}, {@code RESOLVED},
 * {@code DISMISSED}), whether a user or the platform made it.
 *
 * <p>Internal to the {@code monitoring} context. It is consumed, after the transaction commits, by
 * the telemetry stream, which pushes the change to the live dashboards of the unit's owner.</p>
 *
 * @param alertId         identifier of the alert
 * @param equipmentId     identifier of the unit the alert concerns
 * @param type            kind of condition the alert reports
 * @param severity        how urgently the alert must be surfaced
 * @param previousStatus  the status the alert left, or {@code null} when it was just raised
 * @param status          the status the alert entered
 * @param peakTemperature the most extreme temperature recorded for the alert, in Celsius, or
 *                        {@code null} when no temperature is involved
 * @param changedAt       when the alert entered {@code status}
 */
public record AlertStatusChangedEvent(
    Long alertId,
    Long equipmentId,
    AlertType type,
    AlertSeverity severity,
    AlertStatus previousStatus,
    AlertStatus status,
    Double peakTemperature,
    Instant changedAt) {
}
