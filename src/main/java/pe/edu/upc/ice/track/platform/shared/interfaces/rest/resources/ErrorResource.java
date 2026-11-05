package pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/**
 * Standard error response resource returned from REST endpoints.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(
    name = "ErrorResponse",
    description = "Standard error response returned by the REST API",
    example = "{\"code\": \"RESOURCE_NOT_FOUND\", \"message\": \"The requested resource was not found\", "
        + "\"details\": \"Equipment 99 does not exist\"}"
)
public record ErrorResource(
    @Schema(description = "Machine-readable error code", example = "RESOURCE_NOT_FOUND")
    String code,

    @Schema(description = "Human-readable error description",
        example = "The requested resource was not found")
    String message,

    @Schema(description = "Optional additional context; omitted when there is none",
        example = "Equipment 99 does not exist", nullable = true)
    @Nullable String details) {

  /**
   * Creates an ErrorResource from code and message.
   */
  public ErrorResource(String code, String message) {
    this(code, message, null);
  }
}
