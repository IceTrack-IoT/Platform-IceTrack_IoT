package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource received to authenticate an existing user.
 */
@Schema(
    name = "SignInWithLocalRequest",
    description = "User sign-in request with credentials",
    example = "{\"username\": \"john.doe\", \"password\": \"SecurePass123!\"}"
)
public record SignInWithLocalResource(
    @Schema(
        description = "Username",
        example = "john.doe",
        minLength = 3,
        maxLength = 50
    )
    String username,

    @Schema(
        description = "User password",
        example = "SecurePass123!",
        minLength = 8,
        maxLength = 255
    )
    String password
) {
}
