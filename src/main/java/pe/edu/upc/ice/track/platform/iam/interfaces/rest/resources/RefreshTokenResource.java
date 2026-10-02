package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource carrying a refresh token, received to refresh a session or to sign out of it.
 */
@Schema(
    name = "RefreshTokenRequest",
    description = "Request carrying the refresh token issued at sign-in or by a previous refresh",
    example = "{\"refresh_token\": \"x6Kx3Jm0m3o6bRz9P4lXg0f1a2Y8sQ7cV5nT1wE9rU4\"}"
)
public record RefreshTokenResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Opaque refresh token; single use, rotated on every refresh",
        example = "x6Kx3Jm0m3o6bRz9P4lXg0f1a2Y8sQ7cV5nT1wE9rU4",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String refreshToken
) {
}
