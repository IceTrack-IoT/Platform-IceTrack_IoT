package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Resource for a temperature range, both received and returned by the dashboard configuration
 * endpoints.
 */
@Schema(
    name = "TemperatureRange",
    description = "Temperature range a dashboard filters on by default",
    example = "{\"min\": -22, \"max\": -18, \"unit\": \"C\", \"label\": \"-18°C to -22°C\"}"
)
public record TemperatureRangeResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Lower bound of the range, never above max", example = "-22")
    Integer min,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Upper bound of the range, never below min", example = "-18")
    Integer max,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Unit of both bounds", example = "C", allowableValues = {"C", "F"})
    String unit,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Human-readable range label", example = "-18°C to -22°C")
    String label
) {
}
