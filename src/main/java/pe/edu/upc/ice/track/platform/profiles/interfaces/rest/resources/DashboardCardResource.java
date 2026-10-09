package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for a card of a dashboard configuration.
 *
 * <p>The visibility flag is named explicitly: left to the bean conventions, the {@code isVisible}
 * accessor could be read as a property called {@code visible}.</p>
 */
@Schema(
    name = "DashboardCardResponse",
    description = "Dashboard card information response",
    example = "{\"card_id\": 7, \"card_type\": \"OPEN_ALERTS\", \"order\": 1, \"is_visible\": true}"
)
public record DashboardCardResource(
    @Schema(description = "Card unique identifier", example = "7")
    Long cardId,

    @Schema(description = "Kind of widget", example = "OPEN_ALERTS",
        allowableValues = {"MONITORED_EQUIPMENT", "OPEN_ALERTS", "ACTIVE_ORDERS", "EQUIPMENT_STATUS"})
    String cardType,

    @Schema(description = "Read-only position of the card on the dashboard: contiguous, starting at 1, "
        + "assigned on addition and compacted on removal", example = "1", accessMode = Schema.AccessMode.READ_ONLY)
    Integer order,

    @JsonProperty("is_visible")
    @Schema(description = "Whether the card is shown", example = "true")
    boolean isVisible
) {
}
