package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource returned after successful authentication.
 *
 * <p>Contains the authenticated user identifier, username, role, the short-lived bearer token
 * to be used in subsequent API calls, and the refresh token used to renew it. The token carries
 * the same role as its {@code role} claim.</p>
 */
@Schema(
    name = "AuthenticatedUserResponse",
    description = "Authenticated user information with JWT access token and refresh token",
    example = "{\"id\": 1, \"username\": \"john.doe\", \"role\": \"OWNER_ROLE\", \"token\": \"eyJhbGciOiJIUzI1NiIs...\", "
        + "\"refresh_token\": \"x6Kx3Jm0m3o6bRz9P4lXg0f1a2Y8sQ7cV5nT1wE9rU4\"}"
)
public record AuthenticatedUserResource(
    @Schema(description = "User unique identifier", example = "1")
    Long id,

    @Schema(description = "User username", example = "john.doe")
    String username,

    @Schema(description = "Role of the account", example = "OWNER_ROLE", allowableValues = {"OWNER_ROLE", "TECHNICIAN_ROLE"})
    String role,

    @Schema(description = "Short-lived JWT Bearer access token for authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token,

    @Schema(description = "Single-use refresh token, exchanged at /api/v1/authentication/refresh-token for a new token pair",
        example = "x6Kx3Jm0m3o6bRz9P4lXg0f1a2Y8sQ7cV5nT1wE9rU4")
    String refreshToken
) {
}
