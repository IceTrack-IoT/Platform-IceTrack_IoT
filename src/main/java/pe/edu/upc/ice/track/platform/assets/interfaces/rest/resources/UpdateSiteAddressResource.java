package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

/**
 * Resource received to move a site to a newly verified address.
 *
 * <p>The new address is the one carried by {@code validation_token}. The response carries the
 * site's new coordinates immediately, since nothing is looked up in the background any more.</p>
 *
 * @param validationToken token from {@code POST /api/v1/assets/sites/validate-address}
 * @param pin             optional owner-adjusted location, within 150 meters of the verified point
 */
@Schema(
    name = "UpdateSiteAddressRequest",
    description = "Site address change; the new address comes from a validated token",
    example = "{\"validation_token\": \"eyJhbGciOiJIUzI1NiJ9.e30.signature\", \"pin\": null}")
public record UpdateSiteAddressResource(
    @Schema(description = "Token issued by the address validation endpoint, valid for 15 minutes")
    String validationToken,

    @Valid
    @Schema(description = "Owner-adjusted location, within 150 meters of the verified point", nullable = true)
    PinResource pin
) {
}
