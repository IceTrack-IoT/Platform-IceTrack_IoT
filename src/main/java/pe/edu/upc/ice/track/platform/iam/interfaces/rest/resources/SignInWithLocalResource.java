package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Resource received to authenticate an existing user.
 */
@Schema(
    name = "SignInWithLocalRequest",
    description = "User sign-in request with credentials and the client application it comes from",
    example = "{\"username\": \"john.doe\", \"password\": \"SecurePass123!\", \"channel\": \"WEB\"}"
)
public record SignInWithLocalResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 255, message = "{validation.size.max}")
    @Schema(
        description = "Username, or the email address of the account's profile (matched case-insensitively)",
        example = "john.doe",
        maxLength = 255,
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String username,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 255, message = "{validation.size.max}")
    @Schema(
        description = "User password",
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
