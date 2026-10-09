package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resource for the dashboard configuration of a platform account.
 */
@Schema(
    name = "DashboardConfigResponse",
    description = "Dashboard configuration information response",
    example = "{\"id\": 1, \"user_id\": 42, \"default_site_id\": 3, \"default_temperature_range\": {\"min\": -22, \"max\": -18, \"unit\": \"C\", \"label\": \"-18°C to -22°C\"}, \"cards\": [{\"card_id\": 7, \"card_type\": \"OPEN_ALERTS\", \"order\": 1, \"is_visible\": true}]}"
)
public record DashboardConfigResource(
    @Schema(description = "Dashboard configuration unique identifier", example = "1")
    Long id,

    @Schema(description = "Identifier of the platform account the configuration belongs to", example = "42")
    Long userId,

    @Schema(description = "Identifier of the site the dashboard opens on", example = "3")
    Long defaultSiteId,

    @Schema(description = "Temperature range the dashboard opens on")
    TemperatureRangeResource defaultTemperatureRange,

    @Schema(description = "Cards of the dashboard, sorted by their order")
    List<DashboardCardResource> cards
) {
}
