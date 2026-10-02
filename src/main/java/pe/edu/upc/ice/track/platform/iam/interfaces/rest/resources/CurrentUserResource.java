package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resource describing the account of the authenticated principal.
 */
@Schema(
    name = "CurrentUserResponse",
    description = "Identity of the authenticated user",
    example = "{\"id\": 1, \"username\": \"john.doe\", \"email\": \"john.doe@example.com\", \"role\": \"OWNER_ROLE\"}"
)
public record CurrentUserResource(
    @Schema(description = "User unique identifier", example = "1")
    Long id,

    @Schema(description = "User username", example = "john.doe")
    String username,

    @Schema(description = "User email address", example = "john.doe@example.com")
    String email,

    @Schema(description = "User role", example = "[\"OWNER_ROLE\"]")
    String role
) {
}
