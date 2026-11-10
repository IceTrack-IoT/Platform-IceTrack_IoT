package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * One time bucket of a unit's telemetry history: the reading batches taken within it, aggregated.
 */
@Schema(
    name = "TelemetryBucketResponse",
    description = "Reading batches of one time bucket, aggregated",
    example = "{\"bucket_start\": \"2026-11-09T14:00:00Z\", \"bucket_end\": \"2026-11-09T15:00:00Z\", "
        + "\"min_temperature\": 3.1, \"max_temperature\": 5.4, \"avg_temperature\": 4.2, \"avg_humidity\": 61.5, "
        + "\"sample_count\": 360, \"batch_count\": 60, \"under_maintenance\": false, \"door_open_seconds\": 42, "
        + "\"last_received_at\": \"2026-11-09T14:59:58Z\"}")
public record TelemetryBucketResource(
    @Schema(description = "Inclusive start of the bucket", example = "2026-11-09T14:00:00Z")
    Instant bucketStart,

    @Schema(description = "Exclusive end of the bucket", example = "2026-11-09T15:00:00Z")
    Instant bucketEnd,

    @Schema(description = "Minimum of the batches' min_temperature, in Celsius", example = "3.1")
    double minTemperature,

    @Schema(description = "Maximum of the batches' max_temperature, in Celsius", example = "5.4")
    double maxTemperature,

    @Schema(description = "Average of the batches' avg_temperature weighted by sample_count, in Celsius", example = "4.2")
    double avgTemperature,

    @Schema(description = "Weighted average humidity; null when no batch had humidity", example = "61.5", nullable = true)
    Double avgHumidity,

    @Schema(description = "Sum of the batches' samples", example = "360")
    long sampleCount,

    @Schema(description = "Batches aggregated", example = "60")
    int batchCount,

    @Schema(description = "True when any aggregated batch was under maintenance", example = "false")
    boolean underMaintenance,

    @Schema(description = "Sum of the seconds the door was open; null when no batch reported it", example = "42",
        nullable = true)
    Long doorOpenSeconds,

    @Schema(description = "Latest server arrival time in the bucket", example = "2026-11-09T14:59:58Z")
    Instant lastReceivedAt
) {
}
