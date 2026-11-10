package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource returned when a stream ticket is issued.
 */
@Schema(
    name = "StreamTicketResponse",
    description = "Single-use ticket opening the telemetry stream: GET /api/v1/telemetry/stream?ticket=...",
    example = "{\"ticket\": \"st_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08\", \"expires_in\": 60}"
)
public record StreamTicketResource(
    @Schema(description = "Opaque ticket: st_ followed by 64 hexadecimal characters. Shown only in this response",
        example = "st_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
    String ticket,

    @Schema(description = "Seconds the ticket stays redeemable; it opens one stream at most", example = "60")
    long expiresIn
) {
}
