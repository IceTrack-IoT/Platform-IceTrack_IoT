package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Resource received to replace the acceptable temperature band of a unit.
 *
 * <p>Both bounds are required together because a band is only meaningful as a pair. The
 * {@code min < max} rule itself is enforced by the domain, which rejects an inverted band before
 * the aggregate is touched and therefore before any event is raised.</p>
 */
@Schema(
    name = "ChangeThresholdRequest",
    description = "Temperature threshold change request",
    example = "{\"min_celsius\": -22.0, \"max_celsius\": -12.0}")
public record ChangeThresholdResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Lowest acceptable temperature in Celsius, strictly below max_celsius", example = "-22.0")
    Double minCelsius,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Highest acceptable temperature in Celsius, strictly above min_celsius", example = "-12.0")
    Double maxCelsius
) {
}