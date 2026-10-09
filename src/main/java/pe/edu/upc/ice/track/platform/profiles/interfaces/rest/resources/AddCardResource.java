package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Resource received to add a card to a dashboard.
 *
 * <p>Carries no position: the card is appended after the existing ones, and its order is returned
 * in the response.</p>
 */
@Schema(
    name = "AddCardRequest",
    description = "Dashboard card addition request",
    example = "{\"card_type\": \"OPEN_ALERTS\", \"is_visible\": true}"
)
public record AddCardResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Kind of widget, case insensitive", example = "OPEN_ALERTS",
        allowableValues = {"MONITORED_EQUIPMENT", "OPEN_ALERTS", "ACTIVE_ORDERS", "EQUIPMENT_STATUS"})
    String cardType,

    @JsonProperty("is_visible")
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Whether the card is shown", example = "true")
    Boolean isVisible
) {
}
