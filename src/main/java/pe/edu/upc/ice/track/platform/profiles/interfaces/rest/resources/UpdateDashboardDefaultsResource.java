package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Resource received to replace the site and temperature range a dashboard opens on.
 */
@Schema(
    name = "UpdateDefaultsRequest",
    description = "Dashboard defaults update request",
    example = "{\"default_site_id\": 3, \"default_temperature_range\": {\"value\": \"-18_-22\", \"label\": \"-18 °C to -22 °C\"}}"
)
public record UpdateDashboardDefaultsResource(
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
