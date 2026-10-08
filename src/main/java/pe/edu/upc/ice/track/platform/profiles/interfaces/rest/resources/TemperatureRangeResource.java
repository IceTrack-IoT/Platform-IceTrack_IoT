package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource for a temperature range, both received and returned by the dashboard configuration
 * endpoints.
 */
@Schema(
    name = "TemperatureRange",
    description = "Temperature range a dashboard filters on by default",
    example = "{\"value\": \"-18_-22\", \"label\": \"-18 °C to -22 °C\"}"
)
public record TemperatureRangeResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Machine-readable range value", example = "-18_-22")
    String value,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Human-readable range label", example = "-18 °C to -22 °C")
    String label
) {
}
