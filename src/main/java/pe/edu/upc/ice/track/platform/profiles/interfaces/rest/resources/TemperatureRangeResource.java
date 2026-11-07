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
    example = "{\"min\": 2, \"max\": 8, \"unit\": \"C\", \"label\": \"2°C to 8°C\"}"
)
public record TemperatureRangeResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Lower bound of the range, never above max", example = "2")
    Integer min,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Upper bound of the range, never below min", example = "8")
    Integer max,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Unit of both bounds", example = "C", allowableValues = {"C", "F"})
    String unit,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Human-readable range label", example = "2°C to 8°C")
    String label
) {
}
