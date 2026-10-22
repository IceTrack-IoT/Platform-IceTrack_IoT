package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to change the address of a site.
 *
 * <p>Changing the address restarts geocoding, so the response always comes back {@code PENDING}
 * with no location; the new coordinates appear once the background lookup completes.</p>
 */
@Schema(
    name = "UpdateSiteAddressRequest",
    description = "Site address update request",
    example = "{\"address\": \"Av. Los Pinos 45, Lima\"}")
public record UpdateSiteAddressResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 50, message = "{validation.size.max}")
    @Schema(description = "New physical address of the site", example = "Av. Los Pinos 45, Lima", maxLength = 50)
    String address
) {
}
