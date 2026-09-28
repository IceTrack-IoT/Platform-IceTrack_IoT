package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource returned after successful authentication.
 *
 * <p>Contains the authenticated user identifier, username, role, and the bearer token
 * to be used in subsequent API calls. The token carries the same role as its {@code role}
 * claim.</p>
 */
@Schema(
    name = "AuthenticatedUserResponse",
    description = "Authenticated user information with JWT token",
    example = "{\"id\": 1, \"username\": \"john.doe\", \"role\": \"OWNER_ROLE\", \"token\": \"eyJhbGciOiJIUzI1NiIs...\"}"
)
public record AuthenticatedUserResource(
    @Schema(description = "User unique identifier", example = "1")
    Long id,

    @Schema(description = "User username", example = "john.doe")
    String username,

    @Schema(description = "Role of the account", example = "OWNER_ROLE", allowableValues = {"OWNER_ROLE", "TECHNICIAN_ROLE"})
    String role,

    @Schema(description = "JWT Bearer token for authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    String token
) {
}
