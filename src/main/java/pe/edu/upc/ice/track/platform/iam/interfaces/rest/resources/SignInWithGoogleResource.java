package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource received to verify a Google OIDC id_token against the registered accounts.
 *
 * <p>The frontend performs the Google login, obtains the {@code id_token} and posts it here. When
 * the Google account is registered the platform bearer token is returned; otherwise the frontend
 * must show the onboarding form and submit a {@link CompleteGoogleOwnerRegistrationResource} or a
 * {@link CompleteGoogleTechnicianRegistrationResource}.</p>
 */
@Schema(
    name = "SignInWithGoogleRequest",
    description = "Request carrying the Google OIDC id_token obtained by the frontend",
    example = "{\"idToken\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5Y\"}"
)
public record SignInWithGoogleResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Google OIDC id_token issued to the frontend",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5Y",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String idToken
) {
}
