package pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Error resource returned when a refresh token is rejected.
 *
 * <p>{@code code} is the machine-readable contract: clients branch on it and never on
 * {@code message}, which is human-readable and may change.</p>
 */
@Schema(
    name = "AuthErrorResponse",
    description = "Rejection of a refresh token, with a machine-readable code",
    example = "{\"timestamp\": \"2026-10-01T12:00:00Z\", \"status\": 401, \"error\": \"Unauthorized\", "
        + "\"code\": \"REFRESH_TOKEN_RECENTLY_ROTATED\", "
        + "\"message\": \"The provided refresh token was already rotated within the acceptable grace period window.\"}"
)
public record AuthErrorResource(
    @Schema(description = "Instant the error occurred, ISO-8601 in UTC", example = "2026-10-01T12:00:00Z")
    String timestamp,

    @Schema(description = "HTTP status code", example = "401")
    int status,

    @Schema(description = "HTTP status reason phrase", example = "Unauthorized")
    String error,

    @Schema(
        description = "Machine-readable reason. Only REFRESH_TOKEN_RECENTLY_ROTATED is recoverable, by retrying once "
            + "with the latest refresh token; every other code ends the session",
        example = "REFRESH_TOKEN_RECENTLY_ROTATED",
        allowableValues = {
            "REFRESH_TOKEN_RECENTLY_ROTATED",
            "REFRESH_TOKEN_REPLAY_DETECTED",
            "REFRESH_TOKEN_EXPIRED",
            "REFRESH_TOKEN_REVOKED",
            "REFRESH_TOKEN_INVALID"
        })
    String code,

    @Schema(description = "Human-readable description of the error")
    String message
) {
}
