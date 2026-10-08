package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Resource carrying one aggregated sensor reading batch sent by the Edge API.
 */
@Schema(
    name = "RecordReadingBatchRequest",
    description = "Request payload for an aggregated reading batch forwarded by the Edge API",
    example = "{\"readingUid\": \"3fa85f64-5717-4562-b3fc-2c963f66afa6\", \"equipmentId\": 1, "
        + "\"deviceId\": 7, \"minTemperature\": -18.4, \"maxTemperature\": -17.1, "
        + "\"avgTemperature\": -17.8, \"humidity\": 62.0, \"sampleCount\": 12, "
        + "\"recordedAt\": \"2026-09-29T08:30:00\"}"
)
public record RecordReadingBatchResource(
    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Client-generated identifier used to de-duplicate retried deliveries",
        example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    UUID readingUid,

    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Identifier of the equipment the reading belongs to", example = "1")
    Long equipmentId,

    @NotNull(message = "{validation.not-blank}")
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

    @Schema(description = "Number of raw samples folded into this reading", example = "12")
    Integer sampleCount,

    @NotNull(message = "{validation.not-blank}")
    @Schema(description = "Timestamp assigned by the device clock", example = "2026-09-29T08:30:00")
    LocalDateTime recordedAt
) {
}
