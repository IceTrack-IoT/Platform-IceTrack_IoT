package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource for creating a profile.
 */
@Schema(
    name = "CreateProfileRequest",
    description = "Request payload for creating a new profile",
    example = "{\"userId\": 42, \"firstName\": \"John\", \"lastName\": \"Doe\", \"email\": \"john.doe@example.com\", \"countryCode\": \"+51\", \"phoneNumber\": \"987654321\", \"street\": \"123 Main St\", \"number\": \"Apt 4\", \"city\": \"Springfield\", \"postalCode\": \"12345\", \"country\": \"USA\"}"
)
public record CreateProfileResource(
    @Schema(
        description = "Identifier of the platform account this profile belongs to. Omit it to create an unlinked profile.",
        example = "42",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    Long userId,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Profile first name", example = "John", minLength = 1, maxLength = 50)
    String firstName,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Profile last name", example = "Doe", minLength = 1, maxLength = 50)
    String lastName,

    @NotBlank(message = "{validation.not-blank}")
    @Email(message = "{validation.email}")
    @Schema(description = "Profile email address", example = "john.doe@example.com")
    String email,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Country code for phone number", example = "+1", minLength = 1, maxLength = 5)
    String countryCode,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Phone number", example = "987654321", minLength = 1, maxLength = 20)
    String phoneNumber,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Street address", example = "123 Main St", minLength = 1, maxLength = 100)
    String street,

    @Schema(description = "Street number or apartment", example = "Apt 4", minLength = 1, maxLength = 20)
    String number,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "City name", example = "Springfield", minLength = 1, maxLength = 50)
    String city,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Postal code", example = "12345", minLength = 1, maxLength = 20)
    String postalCode,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(description = "Country name", example = "USA", minLength = 1, maxLength = 50)
    String country
) {
}
