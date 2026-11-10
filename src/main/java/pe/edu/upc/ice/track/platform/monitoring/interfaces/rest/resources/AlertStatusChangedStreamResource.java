package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Data of a {@code telemetry-update} stream event announcing that an alert entered a status.
 */
@Schema(
    name = "TelemetryAlertUpdate",
    description = "Data of a telemetry-update event for an alert of one of the owner's units that was raised "
        + "or moved to another status",
    example = "{\"type\": \"ALERT_STATUS_CHANGED\", \"alert_id\": 301, \"equipment_id\": 21, "
        + "\"alert_type\": \"TEMPERATURE_EXCURSION\", \"severity\": \"CRITICAL\", \"previous_status\": \"OPEN\", "
        + "\"status\": \"ACKNOWLEDGED\", \"peak_temperature\": 9.5, \"changed_at\": \"2026-09-29T08:40:00Z\"}"
)
public record AlertStatusChangedStreamResource(
    @Schema(description = "Kind of update; always ALERT_STATUS_CHANGED for this data",
        example = "ALERT_STATUS_CHANGED", allowableValues = {"ALERT_STATUS_CHANGED"})
    String type,

    @Schema(description = "Identifier of the alert", example = "301")
    Long alertId,

    @Schema(description = "Identifier of the unit the alert concerns", example = "21")
    Long equipmentId,

    @Schema(description = "Kind of condition the alert reports", example = "TEMPERATURE_EXCURSION",
        allowableValues = {"TEMPERATURE_EXCURSION", "DEVICE_OFFLINE"})
    String alertType,

    @Schema(description = "How urgently the alert must be surfaced", example = "CRITICAL",
        allowableValues = {"INFO", "WARNING", "CRITICAL"})
    String severity,

    @Schema(description = "Status the alert left; null when the alert was just raised", example = "OPEN",
        nullable = true, allowableValues = {"OPEN", "ACKNOWLEDGED"})
    String previousStatus,

    @Schema(description = "Status the alert entered", example = "ACKNOWLEDGED",
        allowableValues = {"OPEN", "ACKNOWLEDGED", "RESOLVED", "DISMISSED"})
    String status,

    @Schema(description = "Most extreme temperature recorded for the alert, in Celsius; null when no temperature "
        + "is involved", example = "9.5", nullable = true)
    Double peakTemperature,

    @Schema(description = "When the alert entered status", example = "2026-09-29T08:40:00Z")
    Instant changedAt
) {
}
