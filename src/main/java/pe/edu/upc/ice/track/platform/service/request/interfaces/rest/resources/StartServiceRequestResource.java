package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Resource optionally received to start the work on a service request.
 *
 * <p>The whole body is optional. A device working offline reports when the work actually started;
 * without it, the server clock is used.</p>
 */
@Schema(
    name = "StartServiceRequestRequest",
    description = "Service request start request",
    example = "{\"started_at\": \"2026-10-23T14:05:00Z\"}")
public record StartServiceRequestResource(
    @Schema(
        description = "When the work started according to the technician's device, ISO-8601; "
            + "the server clock is used when absent. Repeating the same instant is a no-op.",
        example = "2026-10-23T14:05:00Z",
        nullable = true)
    Instant startedAt
) {
}
