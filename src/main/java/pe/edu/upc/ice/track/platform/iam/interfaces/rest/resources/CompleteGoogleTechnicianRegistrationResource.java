package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource received to complete the deferred registration of a Google account as an ice track
 * maintenance technician.
 *
 * <p>Carries the Google id_token - re-verified by the backend, and the only trusted source of the
 * email and name - together with the technician onboarding form. There is no role field:
 * {@code TECHNICIAN_ROLE} is implied by the endpoint.</p>
 */
@Schema(
    name = "CompleteGoogleTechnicianRegistrationRequest",
    description = "Technician onboarding form completing the registration of a Google account",
    example = "{\"idToken\": \"eyJhbGciOiJSUzI1NiIs...\", \"phone\": \"+51 987654321\", \"street\": \"Av. Primavera\", \"number\": \"123\", \"city\": \"Lima\", \"postalCode\": \"15023\", \"country\": \"Peru\", \"speciality\": \"Refrigeration\", \"certificationNumber\": \"CERT-2024-001\"}"
)
public record CompleteGoogleTechnicianRegistrationResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Google OIDC id_token issued to the frontend", example = "eyJhbGciOiJSUzI1NiIs...",
        requiredMode = Schema.RequiredMode.REQUIRED)
    String idToken,

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

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Technician speciality", example = "Refrigeration")
    String speciality,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Technician certification number", example = "CERT-2024-001")
    String certificationNumber
) {
}
