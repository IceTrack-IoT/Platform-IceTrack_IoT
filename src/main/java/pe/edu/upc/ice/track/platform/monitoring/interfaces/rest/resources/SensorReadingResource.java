package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Resource for a stored sensor reading.
 */
@Schema(
    name = "SensorReadingResponse",
    description = "A stored aggregated sensor reading",
    example = "{\"id\": 501, \"reading_uid\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"equipment_id\": 21, "
        + "\"device_id\": 7, \"min_temperature\": 3.3, \"max_temperature\": 4.6, \"avg_temperature\": 3.9, "
        + "\"raw_min_temperature\": 3.8, \"raw_max_temperature\": 5.1, \"raw_avg_temperature\": 4.4, "
        + "\"humidity\": 62.0, \"sample_count\": 6, \"light_level_max\": 41.0, \"door_open_seconds\": 12, "
        + "\"presence_detected\": true, \"recorded_at\": \"2026-09-29T08:30:00Z\", \"applied_config_version\": 3, "
        + "\"under_maintenance\": false}"
)
public record SensorReadingResource(
    @Schema(description = "Reading unique identifier", example = "501")
    Long id,

    @Schema(description = "Client-generated identifier of the batch", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    String readingUid,

    @Schema(description = "Identifier of the equipment the reading belongs to", example = "21")
    Long equipmentId,

    @Schema(description = "Identifier of the device that produced the reading", example = "7")
    Long deviceId,

    @Schema(description = "Calibrated minimum temperature observed in the window, in Celsius", example = "3.3")
    Double minTemperature,

    @Schema(description = "Calibrated maximum temperature observed in the window, in Celsius", example = "4.6")
    Double maxTemperature,

    @Schema(description = "Calibrated average temperature observed in the window, in Celsius", example = "3.9")
    Double avgTemperature,

    @Schema(description = "Minimum temperature measured by the probe before calibration, in Celsius",
        example = "3.8", nullable = true)
    Double rawMinTemperature,

    @Schema(description = "Maximum temperature measured by the probe before calibration, in Celsius",
        example = "5.1", nullable = true)
    Double rawMaxTemperature,

    @Schema(description = "Average temperature measured by the probe before calibration, in Celsius",
        example = "4.4", nullable = true)
    Double rawAvgTemperature,

    @Schema(description = "Average relative humidity observed in the window, as a percentage", example = "62.0")
    Double humidity,

    @Schema(description = "Number of raw samples folded into this reading", example = "6")
    Integer sampleCount,

    @Schema(description = "Highest light level inside the unit, 0 to 100 %; null when not reported",
        example = "41.0", nullable = true)
    Double lightLevelMax,

    @Schema(description = "Seconds the door was detected open; null when not reported", example = "12", nullable = true)
    Integer doorOpenSeconds,

    @Schema(description = "Whether presence was detected near the unit; null when presence is unavailable",
        example = "true", nullable = true)
    Boolean presenceDetected,

    @Schema(description = "Timestamp assigned by the device clock", example = "2026-09-29T08:30:00Z")
    Instant recordedAt,

    @Schema(description = "Edge configuration version the gateway was running", example = "3", nullable = true)
    Long appliedConfigVersion,

    @Schema(description = "Whether a maintenance window covered recorded_at; such readings never change an alert",
        example = "false")
    boolean underMaintenance
) {
}
