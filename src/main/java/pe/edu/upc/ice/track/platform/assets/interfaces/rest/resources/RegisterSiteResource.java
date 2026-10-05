package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to register a new site.
 *
 * <p>No owner field: the owner is the authenticated caller, so accepting one here would only create
 * a way for a client to try to act on somebody else's behalf.</p>
 */
@Schema(
    name = "RegisterSiteRequest",
    description = "Site registration request",
    example = "{\"name\": \"Sede Central\", \"address\": \"Av. Primavera 123, Lima\", \"contact_name\": \"Maria Lopez\", \"phone\": \"+51 987654321\"}")
public record RegisterSiteResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Site name", example = "Sede Central", maxLength = 30)
    String name,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 50, message = "{validation.size.max}")
    @Schema(description = "Physical address of the site", example = "Av. Primavera 123, Lima", maxLength = 50)
    String address,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Name of the person reachable on site", example = "Maria Lopez", maxLength = 30)
    String contactName,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Contact phone number, digits only with an optional leading '+' and spaces as separators",
        example = "+51 987654321")
    String phone
) {
}