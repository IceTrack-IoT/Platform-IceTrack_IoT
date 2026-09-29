package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Resource received to register an ice track owner with local credentials.
 *
 * <p>There is no role field: {@code OWNER_ROLE} is implied by the endpoint.</p>
 */
@Schema(
    name = "SignUpOwnerRequest",
    description = "Owner sign-up request with credentials and onboarding form",
    example = "{\"username\": \"john.doe\", \"password\": \"SecurePass123!\", \"email\": \"john.doe@example.com\", \"full_name\": \"John Doe\", \"phone\": \"+51 987654321\", \"street\": \"Av. Primavera\", \"number\": \"123\", \"city\": \"Lima\", \"postal_code\": \"15023\", \"country\": \"Peru\", \"ruc\": 20123456789}"
)
public record SignUpOwnerResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Desired username", example = "john.doe", minLength = 3, maxLength = 50)
    String username,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "User password (minimum 8 characters)", example = "SecurePass123!", minLength = 8, maxLength = 255)
    String password,

    @NotBlank(message = "{validation.not-blank}")
    @Email(message = "{validation.email}")
    @Schema(description = "User email address; it also identifies the profile", example = "john.doe@example.com")
    String email,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Full name of the account holder", example = "John Doe")
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
