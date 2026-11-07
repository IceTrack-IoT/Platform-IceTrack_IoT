package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Resource received to register a new site.
 *
 * <p>No owner field: the owner is the authenticated caller. No address field either: the address
 * is the one carried by {@code validation_token}, issued by the address validation endpoint.</p>
 *
 * @param name            site name
 * @param contactName     person reachable on site
 * @param phone           contact phone number
 * @param validationToken token from {@code POST /api/v1/assets/sites/validate-address}
 * @param pin             optional owner-adjusted location, within 150 meters of the verified point
 */
@Schema(
    name = "RegisterSiteRequest",
    description = "Site registration request; the address comes from a validated token",
    example = "{\"name\": \"Sede Central\", \"contact_name\": \"Maria Lopez\", \"phone\": \"+51 987654321\", "
        + "\"validation_token\": \"eyJhbGciOiJIUzI1NiJ9.e30.signature\", \"pin\": {\"latitude\": -12.0932, \"longitude\": -77.0466}}")
public record RegisterSiteResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Site name", example = "Sede Central", maxLength = 30)
    String name,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 30, message = "{validation.size.max}")
    @Schema(description = "Name of the person reachable on site", example = "Maria Lopez", maxLength = 30)
    String contactName,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Contact phone number, digits only with an optional leading '+' and spaces as separators",
        example = "+51 987654321")
    String phone,

    @Schema(description = "Token issued by the address validation endpoint, valid for 15 minutes; "
        + "a missing, forged, expired or foreign token is answered with ADDRESS_VALIDATION_REQUIRED")
    String validationToken,

    @Valid
    @Schema(description = "Owner-adjusted location, within 150 meters of the verified point; "
        + "omit it to use the verified point", nullable = true)
    PinResource pin
) {
}
