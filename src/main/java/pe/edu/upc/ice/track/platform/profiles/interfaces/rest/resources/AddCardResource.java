package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Resource received to add a card to a dashboard.
 */
@Schema(
    name = "AddCardRequest",
    description = "Dashboard card addition request",
    example = "{\"card_type\": \"OPEN_ALERTS\", \"order\": 0, \"is_visible\": true}"
)
public record AddCardResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Kind of widget, case insensitive", example = "OPEN_ALERTS",
        allowableValues = {"MONITORED_EQUIPMENT", "OPEN_ALERTS", "ACTIVE_ORDERS", "EQUIPMENT_STATUS"})
    String cardType,

    @NotNull(message = "{validation.not-null}")
    @PositiveOrZero(message = "{validation.positive-or-zero}")
    @Schema(description = "Position of the card on the dashboard, starting at zero", example = "0")
    Integer order,

    @JsonProperty("is_visible")
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Whether the card is shown", example = "true")
    Boolean isVisible
) {
}
