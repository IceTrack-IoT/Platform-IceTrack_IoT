package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resource for an alert, including its triggering reading and its lifecycle history.
 */
@Schema(
    name = "AlertResponse",
    description = "A thermal excursion or device-silence alert, with the reading that raised it and who moved it "
        + "through its lifecycle and when",
    example = "{\"id\": 301, \"equipment_id\": 1, \"type\": \"TEMPERATURE_EXCURSION\", \"severity\": \"CRITICAL\", "
        + "\"status\": \"DISMISSED\", \"triggering_reading_id\": 8812, \"triggering_temperature\": 9.5, "
        + "\"triggering_recorded_at\": \"2026-09-29T08:31:00\", \"peak_temperature\": 9.5, "
        + "\"opened_at\": \"2026-09-29T08:32:00\", \"acknowledged_by\": 42, "
        + "\"acknowledged_at\": \"2026-09-29T08:40:00\", \"resolved_by\": null, \"resolved_at\": null, "
        + "\"dismissed_by\": 42, \"dismissed_at\": \"2026-09-29T08:45:00\", "
        + "\"dismiss_reason\": \"Planned defrost cycle\", \"dismiss_evidence\": null, "
        + "\"total_duration_seconds\": 780}"
)
public record AlertResource(
    @Schema(description = "Alert unique identifier", example = "301")
    Long id,

    @Schema(description = "Identifier of the equipment the alert concerns", example = "1")
    Long equipmentId,

    @Schema(description = "Kind of anomalous condition raised",
        example = "TEMPERATURE_EXCURSION", allowableValues = {"TEMPERATURE_EXCURSION", "DEVICE_OFFLINE"})
    String type,

    @Schema(description = "How urgently the alert should be surfaced. Thermal excursions are always CRITICAL.",
        example = "CRITICAL", allowableValues = {"INFO", "WARNING", "CRITICAL"})
    String severity,

    @Schema(description = "Current lifecycle status: OPEN, then optionally ACKNOWLEDGED, and finally RESOLVED or "
        + "DISMISSED", example = "OPEN", allowableValues = {"OPEN", "ACKNOWLEDGED", "RESOLVED", "DISMISSED"})
    String status,

    @Schema(description = "Identifier of the sensor reading that raised the alert", example = "8812", nullable = true)
    Long triggeringReadingId,

    @Schema(description = "Out-of-range value of the triggering reading, in Celsius: its maximum above the range, "
        + "its minimum below it; null for a DEVICE_OFFLINE alert", example = "9.5", nullable = true)
    Double triggeringTemperature,

    @Schema(description = "When the triggering reading was recorded", example = "2026-09-29T08:31:00", nullable = true)
    LocalDateTime triggeringRecordedAt,

    @Schema(description = "Peak temperature observed during the excursion, in Celsius; "
        + "null for a DEVICE_OFFLINE alert", example = "9.5", nullable = true)
    Double peakTemperature,

    @Schema(description = "Timestamp the alert was raised", example = "2026-09-29T08:32:00")
    LocalDateTime openedAt,

    @Schema(description = "IAM user identifier of who acknowledged the alert", example = "42", nullable = true)
    Long acknowledgedBy,

    @Schema(description = "When the alert was acknowledged", example = "2026-09-29T08:40:00", nullable = true)
    LocalDateTime acknowledgedAt,

    @Schema(description = "IAM user identifier of who resolved the alert; null while not resolved, or when it was "
        + "resolved automatically because readings returned within range", example = "42", nullable = true)
    Long resolvedBy,

    @Schema(description = "When the alert was resolved", example = "2026-09-29T09:10:00", nullable = true)
    LocalDateTime resolvedAt,

    @Schema(description = "IAM user identifier of who dismissed the alert", example = "42", nullable = true)
    Long dismissedBy,

    @Schema(description = "When the alert was dismissed", example = "2026-09-29T08:45:00", nullable = true)
    LocalDateTime dismissedAt,

    @Schema(description = "Justification given when the alert was dismissed", example = "Planned defrost cycle",
        nullable = true)
    String dismissReason,

    @Schema(description = "Evidence given when the alert was dismissed", nullable = true)
    String dismissEvidence,

    @Schema(description = "Seconds between opening and resolution or dismissal; null while the alert is active",
        example = "780", nullable = true)
    Long totalDurationSeconds
) {
}
