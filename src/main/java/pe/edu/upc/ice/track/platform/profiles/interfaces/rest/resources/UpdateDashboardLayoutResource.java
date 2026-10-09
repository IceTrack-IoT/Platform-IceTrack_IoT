package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Resource received to replace the card layout of a dashboard.
 *
 * <p>The layout is replaced as a whole: it must place every card of the dashboard exactly once, at
 * the positions {@code 1..N}. That rule is enforced by the dashboard configuration itself; only the
 * shape of each entry is checked here.</p>
 */
@Schema(
    name = "UpdateDashboardLayoutRequest",
    description = "Dashboard layout update request",
    example = "{\"cards\": [{\"card_id\": 8, \"order\": 1, \"is_visible\": true}, {\"card_id\": 7, \"order\": 2, \"is_visible\": false}, {\"card_id\": 9, \"order\": 3, \"is_visible\": true}, {\"card_id\": 10, \"order\": 4, \"is_visible\": true}]}"
)
public record UpdateDashboardLayoutResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Placement of every card of the dashboard")
    List<@NotNull(message = "{validation.not-null}") @Valid CardLayoutItemResource> cards
) {
}
