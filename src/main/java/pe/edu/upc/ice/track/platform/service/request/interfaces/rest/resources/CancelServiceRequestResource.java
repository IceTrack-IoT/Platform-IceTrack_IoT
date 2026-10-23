package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to cancel a service request.
 */
@Schema(
    name = "CancelServiceRequestRequest",
    description = "Service request cancellation request",
    example = "{\"reason\": \"The unit was replaced\"}")
public record CancelServiceRequestResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 500, message = "{validation.size.max}")
    @Schema(description = "Why the owner cancels the request", example = "The unit was replaced", maxLength = 500)
    String reason
) {
}
