package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Resource received to replace the acceptable temperature band of a unit.
 *
 * <p>Both bounds are required together because a band is only meaningful as a pair, and each must
 * lie within 0 to 30 °C, the positive refrigeration range the platform monitors. The
 * {@code min < max} rule itself is enforced by the domain, which rejects an inverted band with
 * "minimum temperature must be lower than maximum" before the aggregate is touched and therefore
 * before any event is raised.</p>
 */
@Schema(
    name = "ChangeThresholdRequest",
    description = "Temperature threshold change request; both bounds within 0 to 30 °C, min strictly below max",
    example = "{\"min_celsius\": 2.0, \"max_celsius\": 8.0}")
public record ChangeThresholdResource(
    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.threshold.range}")
    @DecimalMax(value = "30.0", message = "{validation.threshold.range}")
    @Schema(description = "Lowest acceptable temperature in Celsius, within 0 to 30 and strictly below max_celsius",
        example = "2.0", minimum = "0", maximum = "30")
    Double minCelsius,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "0.0", message = "{validation.threshold.range}")
    @DecimalMax(value = "30.0", message = "{validation.threshold.range}")
    @Schema(description = "Highest acceptable temperature in Celsius, within 0 to 30 and strictly above min_celsius",
        example = "8.0", minimum = "0", maximum = "30")
    Double maxCelsius
) {
}
