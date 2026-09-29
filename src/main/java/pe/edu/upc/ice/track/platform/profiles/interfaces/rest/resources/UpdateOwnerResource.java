package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Resource received to replace the editable details of an owner.
 *
 * <p>A full replacement: every mandatory field must be supplied, including the RUC, so an owner
 * can never be left without one. The email address is not editable here - it belongs to the
 * platform account.</p>
 */
@Schema(
    name = "UpdateOwnerRequest",
    description = "Owner update request",
    example = "{\"fullName\": \"John Doe\", \"phone\": \"+51 987654321\", \"street\": \"Av. Primavera\", \"number\": \"123\", \"city\": \"Lima\", \"postalCode\": \"15023\", \"country\": \"Peru\", \"ruc\": 20123456789}"
)
public record UpdateOwnerResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Owner full name", example = "John Doe")
    String fullName,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Phone number, optionally prefixed by the country code and a space", example = "+51 987654321")
    String phone,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Street of the address", example = "Av. Primavera")
    String street,

    @Schema(description = "Street number or apartment", example = "123", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String number,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "City of the address", example = "Lima")
    String city,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Postal code of the address", example = "15023")
    String postalCode,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Country of the address", example = "Peru")
    String country,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "11 digit taxpayer registration number (RUC)", example = "20123456789")
    Long ruc
) {
}
