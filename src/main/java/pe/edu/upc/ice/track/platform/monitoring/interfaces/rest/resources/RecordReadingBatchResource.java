package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.UUID;

/**
 * Resource carrying one aggregated sensor reading batch sent by an edge device.
 *
 * <p>The device and equipment are identified by the API key, not by this payload.
 * {@code equipment_id} and {@code device_id} may still be sent, but only as a cross-check: when
 * present they must match the authenticated device, or the request is refused with 403.</p>
 *
 * <p>Temperatures come twice. {@code raw_*} is what the DHT11 probe measured and must lie within
 * 0 to 50 °C. {@code min/max/avg_temperature} are the raw values corrected by the calibration
 * offset the edge applied (thresholds are evaluated on them), so each must lie within 5 °C of its
 * raw value. Both sets must satisfy {@code min <= avg <= max}. Humidity must lie within 20 to
 * 90 %. Light level, door-open time and presence are optional; an omitted presence is stored as
 * unavailable, never as "no presence". Any violation, or a non-numeric value, is answered with
 * 400 "Invalid reading payload" and nothing is stored.</p>
 */
@Schema(
    name = "RecordReadingBatchRequest",
    description = "Aggregated reading batch sent by an edge device authenticated by its X-API-Key",
    example = "{\"reading_uid\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"equipment_id\": 21, "
        + "\"device_id\": 7, \"min_temperature\": 3.3, \"max_temperature\": 4.6, "
        + "\"avg_temperature\": 3.9, \"raw_min_temperature\": 3.8, \"raw_max_temperature\": 5.1, "
        + "\"raw_avg_temperature\": 4.4, \"humidity\": 62.0, \"sample_count\": 6, "
        + "\"light_level_max\": 41.0, \"door_open_seconds\": 12, \"presence_detected\": true, "
        + "\"recorded_at\": \"2026-09-29T08:30:00Z\", \"applied_config_version\": 3}"
)
public record RecordReadingBatchResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Client-generated identifier used to de-duplicate retried deliveries",
        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID readingUid,

    @Schema(description = "Optional cross-check: must equal the equipment the authenticated device is paired with",
        example = "21", nullable = true)
    Long equipmentId,

    @Schema(description = "Optional cross-check: must equal the authenticated device", example = "7", nullable = true)
    Long deviceId,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "-5.0", message = "{validation.reading.calibrated-temperature}")
    @DecimalMax(value = "55.0", message = "{validation.reading.calibrated-temperature}")
    @Schema(description = "Calibrated minimum temperature in the window, in Celsius; within 5 of raw_min_temperature",
        example = "3.3", minimum = "-5", maximum = "55")
    Double minTemperature,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "-5.0", message = "{validation.reading.calibrated-temperature}")
    @DecimalMax(value = "55.0", message = "{validation.reading.calibrated-temperature}")
    @Schema(description = "Calibrated maximum temperature in the window, in Celsius; within 5 of raw_max_temperature",
        example = "4.6", minimum = "-5", maximum = "55")
    Double maxTemperature,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "-5.0", message = "{validation.reading.calibrated-temperature}")
    @DecimalMax(value = "55.0", message = "{validation.reading.calibrated-temperature}")
    @Schema(description = "Calibrated average temperature in the window, in Celsius; within 5 of raw_avg_temperature",
        example = "3.9", minimum = "-5", maximum = "55")
    Double avgTemperature,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.reading.temperature}")
    @DecimalMax(value = "50.0", message = "{validation.reading.temperature}")
    @Schema(description = "Minimum temperature measured by the probe before calibration, in Celsius (0 to 50)",
        example = "3.8", minimum = "0", maximum = "50")
    Double rawMinTemperature,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.reading.temperature}")
    @DecimalMax(value = "50.0", message = "{validation.reading.temperature}")
    @Schema(description = "Maximum temperature measured by the probe before calibration, in Celsius (0 to 50)",
        example = "5.1", minimum = "0", maximum = "50")
    Double rawMaxTemperature,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.reading.temperature}")
    @DecimalMax(value = "50.0", message = "{validation.reading.temperature}")
    @Schema(description = "Average temperature measured by the probe before calibration, in Celsius (0 to 50)",
        example = "4.4", minimum = "0", maximum = "50")
    Double rawAvgTemperature,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "20.0", message = "{validation.reading.humidity}")
    @DecimalMax(value = "90.0", message = "{validation.reading.humidity}")
    @Schema(description = "Average relative humidity observed in the window, as a percentage (20 to 90)",
        example = "62.0", minimum = "20", maximum = "90")
    Double humidity,

    @NotNull(message = "{validation.not-null}")
    @Min(value = 1, message = "{validation.reading.sample-count}")
    @Max(value = 360, message = "{validation.reading.sample-count}")
    @Schema(description = "Number of raw samples folded into this reading (1 to 360)", example = "6")
    Integer sampleCount,

    @DecimalMin(value = "0.0", message = "{validation.reading.light-level}")
    @DecimalMax(value = "100.0", message = "{validation.reading.light-level}")
    @Schema(description = "Highest light level inside the unit during the window, 0 to 100 %; omitted when "
        + "the device has no light sensor", example = "41.0", minimum = "0", maximum = "100", nullable = true)
    Double lightLevelMax,

    @PositiveOrZero(message = "{validation.positive-or-zero}")
    @Schema(description = "Seconds the door was detected open during the window; omitted when not reported",
        example = "12", minimum = "0", nullable = true)
    Integer doorOpenSeconds,

    @Schema(description = "Whether the PIR sensor detected presence near the unit during the window; "
        + "omitted when presence is unavailable, which is stored as unavailable, not as false",
        example = "true", nullable = true)
    Boolean presenceDetected,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "When the device took the batch, by its clock; ISO 8601 with Z or an explicit offset; a value without offset is rejected", example = "2026-09-29T08:30:00Z")
    Instant recordedAt,

    @NotNull(message = "{validation.not-null}")
    @PositiveOrZero(message = "{validation.positive-or-zero}")
    @Schema(description = "Edge configuration version the gateway was running when it built this batch",
        example = "3", minimum = "0")
    Long appliedConfigVersion
) {

  /** Largest calibration offset, in either direction, between a calibrated value and its raw value. */
  private static final double MAX_CALIBRATION_OFFSET_CELSIUS = 5.0;

  /** Enforces {@code min <= avg <= max} on the calibrated temperatures. */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "{validation.reading.order}")
  public boolean isTemperatureOrderConsistent() {
    return isOrdered(minTemperature, avgTemperature, maxTemperature);
  }

  /** Enforces {@code min <= avg <= max} on the raw temperatures. */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "{validation.reading.raw-order}")
  public boolean isRawTemperatureOrderConsistent() {
    return isOrdered(rawMinTemperature, rawAvgTemperature, rawMaxTemperature);
  }

  /** Enforces that each calibrated temperature lies within the calibration offset of its raw value. */
  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "{validation.reading.calibration-offset}")
  public boolean isCalibrationOffsetWithinRange() {
    return isWithinOffset(minTemperature, rawMinTemperature)
        && isWithinOffset(maxTemperature, rawMaxTemperature)
        && isWithinOffset(avgTemperature, rawAvgTemperature);
  }

  // Missing values are reported by their own @NotNull constraints, so they pass here.
  private static boolean isOrdered(Double min, Double avg, Double max) {
    return min == null || avg == null || max == null || (min <= avg && avg <= max);
  }

  private static boolean isWithinOffset(Double calibrated, Double raw) {
    return calibrated == null || raw == null || Math.abs(calibrated - raw) <= MAX_CALIBRATION_OFFSET_CELSIUS;
  }
}
