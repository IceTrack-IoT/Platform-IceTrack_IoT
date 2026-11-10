package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource received to link a Google identity to an account registered with a local password.
 *
 * <p>Sent after {@code /google/verify} answered {@code ACCOUNT_LINK_REQUIRED}: the account holder
 * authorizes the link by presenting the account's current password. The account is resolved from
 * the verified Google email carried by the token, so no username is sent.</p>
 */
@Schema(
    name = "LinkGoogleAccountRequest",
    description = "Request authorizing the link of a Google identity to an existing local account",
    example = "{\"id_token\": \"eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5Y\", "
        + "\"password\": \"SecurePass123!\", \"channel\": \"WEB\"}"
)
public record LinkGoogleAccountResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Google OIDC id_token issued to the frontend",
        example = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjE2YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5YzQ0YjA3ZjM0YjY5Y",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String idToken,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 255, message = "{validation.size.max}")
    @Schema(
        description = "Current local password of the account whose profile uses the Google email",
        example = "SecurePass123!",
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String password,

    @NotNull(message = "{validation.not-null}")
    @Schema(
        description = "Client application: WEB for owners, MOBILE for technicians",
        example = "WEB",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    ClientChannelResource channel
) {
}
