package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource received to register an ice track maintenance technician with local credentials.
 *
 * <p>There is no role field: {@code TECHNICIAN_ROLE} is implied by the endpoint.</p>
 */
@Schema(
    name = "SignUpTechnicianRequest",
    description = "Technician sign-up request with credentials and onboarding form",
    example = "{\"username\": \"jane.doe\", \"password\": \"SecurePass123!\", \"email\": \"jane.doe@example.com\", \"fullName\": \"Jane Doe\", \"phone\": \"+51 987654321\", \"street\": \"Av. Primavera\", \"number\": \"123\", \"city\": \"Lima\", \"postalCode\": \"15023\", \"country\": \"Peru\", \"speciality\": \"Refrigeration\", \"certificationNumber\": \"CERT-2024-001\"}"
)
public record SignUpTechnicianResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Desired username", example = "jane.doe", minLength = 3, maxLength = 50)
    String username,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "User password (minimum 8 characters)", example = "SecurePass123!", minLength = 8, maxLength = 255)
    String password,

    @NotBlank(message = "{validation.not-blank}")
    @Email(message = "{validation.email}")
    @Schema(description = "User email address; it also identifies the profile", example = "jane.doe@example.com")
    String email,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Full name of the account holder", example = "Jane Doe")
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

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Technician speciality", example = "Refrigeration")
    String speciality,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Technician certification number", example = "CERT-2024-001")
    String certificationNumber
) {
}
