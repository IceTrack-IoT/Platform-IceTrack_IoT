package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Resource received for the placement of one card within a dashboard layout update.
 */
@Schema(
    name = "CardLayoutItemRequest",
    description = "Placement of one dashboard card",
    example = "{\"card_id\": 7, \"order\": 1, \"is_visible\": true}"
)
public record CardLayoutItemResource(
    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Card unique identifier", example = "7")
    Long cardId,

    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Requested 1-based position of the card; the positions of a layout must be exactly 1 to N",
        example = "1")
    Integer order,

    @JsonProperty("is_visible")
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Whether the card is shown", example = "true")
    Boolean isVisible
) {
}
