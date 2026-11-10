package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to reject a service request.
 */
@Schema(
    name = "RejectServiceRequestRequest",
    description = "Service request rejection request",
    example = "{\"reason\": \"I do not service this brand of unit\"}")
public record RejectServiceRequestResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 500, message = "{validation.size.max}")
    @Schema(description = "Why the technician rejects the request", example = "I do not service this brand of unit", maxLength = 500)
    String reason
) {
}
