package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resource for a stored sensor reading.
 */
@Schema(name = "SensorReadingResponse", description = "A stored aggregated sensor reading")
public record SensorReadingResource(
    @Schema(description = "Reading unique identifier", example = "501")
    Long id,

    @Schema(description = "Identifier of the equipment the reading belongs to", example = "1")
    Long equipmentId,

    @Schema(description = "Identifier of the device that produced the reading", example = "7")
    Long deviceId,

    @Schema(description = "Minimum temperature observed in the window, in Celsius", example = "-18.4")
    Double minTemperature,

    @Schema(description = "Maximum temperature observed in the window, in Celsius", example = "-17.1")
    Double maxTemperature,

    @Schema(description = "Average temperature observed in the window, in Celsius", example = "-17.8")
    Double avgTemperature,

    @Schema(description = "Average relative humidity observed in the window, as a percentage", example = "62.0")
    Double humidity,

    @Schema(description = "Timestamp assigned by the device clock", example = "2026-09-29T08:30:00")
    LocalDateTime recordedAt
) {
}
