package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Resource received to review a completed service request.
 */
@Schema(
    name = "SubmitReviewRequest",
    description = "Review submission request",
    example = "{\"service_request_id\": 42, \"communication\": 5, \"efficacy\": 4, \"performance\": 5, \"comment\": \"Quick and clear\"}")
public record SubmitReviewResource(
    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Identifier of the completed service request", example = "42")
    Long serviceRequestId,

    @NotNull(message = "{validation.not-null}")
    @Min(1)
    @Max(5)
    @Schema(description = "How well the technician kept the owner informed, from 1 to 5", example = "5", minimum = "1", maximum = "5")
    Integer communication,

    @NotNull(message = "{validation.not-null}")
    @Min(1)
    @Max(5)
    @Schema(description = "Whether the intervention solved the problem, from 1 to 5", example = "4", minimum = "1", maximum = "5")
    Integer efficacy,

    @NotNull(message = "{validation.not-null}")
    @Min(1)
    @Max(5)
    @Schema(description = "How well the work itself was carried out, from 1 to 5", example = "5", minimum = "1", maximum = "5")
    Integer performance,

    @Size(max = 1000, message = "{validation.size.max}")
    @Schema(description = "Free-text comment", example = "Quick and clear", maxLength = 1000, nullable = true)
    String comment
) {
}
