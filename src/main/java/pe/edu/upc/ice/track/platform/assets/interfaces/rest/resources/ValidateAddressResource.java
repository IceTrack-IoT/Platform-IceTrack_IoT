package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to verify an address before saving a site.
 *
 * @param address the free-form address; required, 1 to 255 characters
 * @param placeId the place identifier of a suggestion the owner picked, optional
 */
@Schema(
    name = "ValidateAddressRequest",
    description = "Address to verify with Google Maps before saving a site",
    example = "{\"address\": \"Av. Primavera 123, Santiago de Surco\", \"place_id\": null}")
public record ValidateAddressResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(min = 1, max = 255, message = "{validation.size.max}")
    @Schema(description = "Free-form address to verify", example = "Av. Primavera 123, Santiago de Surco",
        minLength = 1, maxLength = 255)
    String address,

    @Size(max = 256, message = "{validation.size.max}")
    @Schema(description = "Place identifier of the suggestion the owner picked, when there is one",
        example = "ChIJ8e8c9yLIBZERt0P0wP0xV5A", nullable = true)
    String placeId
) {
}
