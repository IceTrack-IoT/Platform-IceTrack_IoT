package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * The owner's adjusted pin for a site. It must lie within 150 meters of the verified point; when it
 * is omitted, the verified point is saved as the site location.
 *
 * @param latitude  latitude of the pin, in degrees
 * @param longitude longitude of the pin, in degrees
 */
@Schema(
    name = "PinRequest",
    description = "Owner-adjusted location of the site, within 150 meters of the verified address")
public record PinResource(
    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "-90.0", message = "{validation.pin.latitude}")
    @DecimalMax(value = "90.0", message = "{validation.pin.latitude}")
    @Schema(description = "Latitude of the pin", example = "-12.0932")
    Double latitude,

    @NotNull(message = "{validation.not-null}")
    @DecimalMin(value = "-180.0", message = "{validation.pin.longitude}")
    @DecimalMax(value = "180.0", message = "{validation.pin.longitude}")
    @Schema(description = "Longitude of the pin", example = "-77.0466")
    Double longitude
) {
}
