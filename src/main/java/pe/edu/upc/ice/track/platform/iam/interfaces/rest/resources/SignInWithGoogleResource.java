package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Resource received to exchange a Google OIDC id_token for a platform bearer token.
 *
 * <p>The frontend performs the Google login, obtains the {@code id_token} and posts it here.
 * The backend validates the token against Google's JWKS and signs the user in, registering the
 * account on first contact.</p>
 */
@Schema(
    name = "SignInWithGoogleRequest",
    description = "Token exchange request carrying the Google OIDC id_token obtained by the frontend",
    example = "{\"idToken\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5Y\", \"requestedRole\": \"OWNER_ROLE\"}"
)
public record SignInWithGoogleResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Google OIDC id_token issued to the frontend",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5Y",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String idToken,

    @Schema(
        description = "Role to assign when the Google account is registered for the first time. "
            + "Ignored for accounts that already exist; defaults to USER_ROLE when omitted.",
        example = "OWNER_ROLE",
        allowableValues = {"USER_ROLE", "OWNER_ROLE", "TECHNICIAN_ROLE"},
        requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    String requestedRole
) {
}
