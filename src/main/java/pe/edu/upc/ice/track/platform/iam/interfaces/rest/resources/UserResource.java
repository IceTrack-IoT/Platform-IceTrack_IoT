package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resource representing an IAM user returned by the REST API.
 */
@Schema(
    name = "UserResponse",
    description = "User information response",
    example = "{\"id\": 1, \"username\": \"john.doe\", \"role\": \"OWNER_ROLE\"}"
)
public record UserResource(
    @Schema(description = "User unique identifier", example = "1")
    Long id,

    @Schema(description = "User username", example = "john.doe")
    String username,

    @Schema(description = "Role of the account", example = "OWNER_ROLE", allowableValues = {"OWNER_ROLE", "TECHNICIAN_ROLE"})
    String role
) {
}
