package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource for a profile.
 */
@Schema(
    name = "ProfileResponse",
    description = "Profile information response",
    example = "{\"id\": 1, \"userId\": 42, \"fullName\": \"John Doe\", \"email\": \"john.doe@example.com\", \"role\": \"OWNER\", \"phoneNumber\": \"+51 987654321\", \"streetAddress\": \"123 Main St, Apt 4, Springfield, 12345, USA\"}"
)
public record ProfileResource(
    @Schema(description = "Profile unique identifier", example = "1")
    Long id,

    @Schema(description = "User unique identifier associated with the profile", example = "42")
    Long userId,

    @Schema(description = "Profile full name", example = "John Doe")
    String fullName,

    @Schema(description = "Profile email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "Role the profile plays", example = "OWNER", allowableValues = {"USER", "OWNER", "TECHNICIAN"})
    String role,

    @Schema(description = "Profile phone number; null until the owner supplies it", example = "+51 987654321")
    String phoneNumber,

    @Schema(description = "Complete street address; null until the owner supplies it", example = "123 Main St, Apt 4, Springfield, 12345, USA")
    String streetAddress,

    @Schema(description = "Opaque annotation supplied by the context that requested the profile, such as the identity provider avatar URL", example = "https://lh3.googleusercontent.com/a/default-user")
    String auxiliaryData
) {
}
