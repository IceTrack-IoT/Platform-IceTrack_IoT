package pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource used for simple success or informational REST responses.
 */
@Schema(
    name = "MessageResponse",
    description = "Simple success or informational message returned by the REST API",
    example = "{\"message\": \"Operation completed successfully\"}"
)
public record MessageResource(
    @Schema(description = "Human-readable message", example = "Operation completed successfully")
    String message) {
}
