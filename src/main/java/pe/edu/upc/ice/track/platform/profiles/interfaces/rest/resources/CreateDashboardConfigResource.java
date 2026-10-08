package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Resource received to create the dashboard configuration of a platform account.
 *
 * <p>The configuration starts with no cards; they are added afterwards, one at a time.</p>
 */
@Schema(
    name = "CreateDashboardConfigRequest",
    description = "Dashboard configuration creation request",
    example = "{\"user_id\": 42, \"default_site_id\": 3, \"default_temperature_range\": {\"value\": \"-18_-22\", \"label\": \"-18 °C to -22 °C\"}}"
)
public record CreateDashboardConfigResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Identifier of the platform account the configuration belongs to", example = "42")
    Long userId,

    @NotNull(message = "{validation.not-null}")
    @Positive(message = "{validation.positive}")
    @Schema(description = "Identifier of the site the dashboard opens on", example = "3")
    Long defaultSiteId,

    @Valid
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Temperature range the dashboard opens on")
    TemperatureRangeResource defaultTemperatureRange
) {
}
