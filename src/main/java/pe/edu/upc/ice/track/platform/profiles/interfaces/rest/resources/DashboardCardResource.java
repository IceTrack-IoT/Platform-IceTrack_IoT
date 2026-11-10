package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for a card of a dashboard configuration.
 *
 * <p>The visibility flag is exposed as {@code is_visible}: the platform-wide {@code SNAKE_CASE}
 * strategy turns the {@code isVisible} record component into that JSON property.</p>
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

    @Schema(description = "Position of the card on the dashboard: the cards of a dashboard always hold the "
        + "positions 1 to N, with no gap and no repeat", example = "1")
    Integer order,

    @Schema(description = "Whether the card is shown; a hidden card keeps its data", example = "true")
    boolean isVisible
) {
}
