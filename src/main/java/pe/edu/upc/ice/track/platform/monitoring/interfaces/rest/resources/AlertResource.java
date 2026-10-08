package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resource for an alert.
 */
@Schema(name = "AlertResponse", description = "A thermal excursion or device-silence alert")
public record AlertResource(
    @Schema(description = "Alert unique identifier", example = "301")
    Long id,

    @Schema(description = "Identifier of the equipment the alert concerns", example = "1")
    Long equipmentId,

    @Schema(description = "Kind of anomalous condition raised",
        example = "TEMPERATURE_EXCURSION", allowableValues = {"TEMPERATURE_EXCURSION", "DEVICE_OFFLINE"})
    String type,

    @Schema(description = "How urgently the alert should be surfaced",
        example = "WARNING", allowableValues = {"INFO", "WARNING", "CRITICAL"})
    String severity,

    @Schema(description = "Current lifecycle status of the alert",
        example = "OPEN", allowableValues = {"OPEN", "ACKNOWLEDGED", "RESOLVED", "DISMISSED"})
    String status,

    @Schema(description = "Peak temperature observed during the excursion, in Celsius; "
        + "null for a DEVICE_OFFLINE alert", example = "-12.5")
    Double peakTemperature,

    @Schema(description = "Timestamp the alert was raised", example = "2026-09-29T08:32:00")
    LocalDateTime openedAt,

    @Schema(description = "Timestamp the alert was resolved or dismissed; null while open or acknowledged",
        example = "2026-09-29T09:10:00")
    LocalDateTime resolvedAt
) {
}
