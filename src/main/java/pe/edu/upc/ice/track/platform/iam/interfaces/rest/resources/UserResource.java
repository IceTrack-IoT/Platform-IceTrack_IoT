package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Resource representing an IAM user returned by the REST API.
 */
@Schema(
    name = "UserResponse",
    description = "User information response",
    example = "{\"id\": 1, \"username\": \"john.doe\", \"roles\": [\"USER_ROLE\", \"OWNER_ROLE\", \"TECHNICIAN_ROLE\"]}"
)
public record UserResource(
    @Schema(description = "User unique identifier", example = "1")
    Long id,

    @Schema(description = "User username", example = "john.doe")
    String username,

    @Schema(description = "User assigned roles", example = "[\"USER_ROLE\"]")
    List<String> roles
) {
}
