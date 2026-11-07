package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Data of a {@code telemetry-update} stream event announcing a reading batch just recorded.
 */
@Schema(
    name = "TelemetryReadingUpdate",
    description = "Data of a telemetry-update event for a reading batch just recorded on one of the owner's units",
    example = "{\"type\": \"READING_RECORDED\", \"reading_uid\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", "
        + "\"equipment_id\": 21, \"device_id\": 7, \"min_temperature\": 3.3, \"max_temperature\": 4.6, "
        + "\"avg_temperature\": 3.9, \"humidity\": 62.0, \"sample_count\": 6, "
        + "\"recorded_at\": \"2026-09-29T08:30:00\", \"received_at\": \"2026-09-29T08:30:02\", "
        + "\"under_maintenance\": false}"
)
public record ReadingRecordedStreamResource(
    @Schema(description = "Kind of update; always READING_RECORDED for this data",
        example = "READING_RECORDED", allowableValues = {"READING_RECORDED"})
    String type,

    @Schema(description = "Client-generated identifier of the batch, unique across the platform",
        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String readingUid,

    @Schema(description = "Identifier of the unit the batch was taken on", example = "21")
    Long equipmentId,

    @Schema(description = "Identifier of the device that produced the batch", example = "7")
    Long deviceId,

    @Schema(description = "Calibrated minimum temperature, in Celsius", example = "3.3")
    Double minTemperature,

    @Schema(description = "Calibrated maximum temperature, in Celsius", example = "4.6")
    Double maxTemperature,

    @Schema(description = "Calibrated average temperature, in Celsius", example = "3.9")
    Double avgTemperature,

    @Schema(description = "Average relative humidity, as a percentage", example = "62.0")
    Double humidity,

    @Schema(description = "Number of raw samples folded into the batch", example = "6")
    Integer sampleCount,

    @Schema(description = "When the batch was taken, by the device clock", example = "2026-09-29T08:30:00")
    LocalDateTime recordedAt,

    @Schema(description = "When the platform received the batch", example = "2026-09-29T08:30:02")
    LocalDateTime receivedAt,

    @Schema(description = "Whether a maintenance window covered recorded_at", example = "false")
    boolean underMaintenance
) {
}
