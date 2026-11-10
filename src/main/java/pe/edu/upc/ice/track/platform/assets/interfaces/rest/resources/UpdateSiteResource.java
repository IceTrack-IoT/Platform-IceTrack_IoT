package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to replace the editable details of a site.
 *
 * <p>A full replacement of name, contact name and phone. The address is not among them: it changes
 * only through {@link UpdateSiteAddressResource}, with a verified token.</p>
 */
@Schema(
    name = "UpdateSiteRequest",
    description = "Site update request (name and contact details; the address is changed separately)",
    example = "{\"name\": \"Sede Norte\", \"contact_name\": \"Carlos Diaz\", \"phone\": \"+51 912345678\"}")
public record UpdateSiteResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Site name", example = "Sede Norte", maxLength = 30)
    String name,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Name of the person reachable on site", example = "Carlos Diaz", maxLength = 30)
    String contactName,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Contact phone number, digits only with an optional leading '+' and spaces as separators",
        example = "+51 912345678")
    String phone
) {
}
