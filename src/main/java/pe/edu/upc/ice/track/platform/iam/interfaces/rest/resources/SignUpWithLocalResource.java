package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Resource received to register a new IAM user.
 */
@Schema(
    name = "SignUpWithLocalRequest",
    description = "User sign-up request with credentials and roles",
    example = "{\"username\": \"john.doe\", \"password\": \"SecurePass123!\", \"email\": \"john.doe@example.com\", \"roles\": [\"USER_ROLE\"]}"
)
public record SignUpWithLocalResource(
    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "Desired username",
        example = "john.doe",
        minLength = 3,
        maxLength = 50
    )
    String username,

    @NotBlank(message = "{validation.not-blank}")
    @Schema(
        description = "User password (minimum 8 characters)",
        example = "SecurePass123!",
        minLength = 8,
        maxLength = 255
    )
    String password,

    @NotBlank(message = "{validation.not-blank}")
    @Email(message = "{validation.email}")
    @Schema(
        description = "User email address. Required: it identifies the profile automatically "
            + "created for this account in the profiles bounded context.",
        example = "john.doe@example.com",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    String email,

    @Schema(
        description = "Roles to assign to the user",
        example = "[\"USER_ROLE\"]",
        allowableValues = {"USER_ROLE", "OWNER_ROLE", "TECHNICIAN_ROLE"}
    )
    List<String> roles
) {
}
