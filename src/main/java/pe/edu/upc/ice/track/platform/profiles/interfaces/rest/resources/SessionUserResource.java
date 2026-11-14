package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The signed-in account inside a session context.
 *
 * <p>Same shape as the {@code CurrentUserResponse} of {@code GET /api/v1/authentication/me}, so a
 * client maps both with one assembler.</p>
 */
@Schema(
    name = "SessionUser",
    description = "The signed-in account; the same shape as CurrentUserResponse",
    example = "{\"id\": 42, \"username\": \"maria\", \"email\": \"maria.lopez@example.com\", "
        + "\"role\": \"OWNER_ROLE\", \"provider\": \"LOCAL\"}")
public record SessionUserResource(
    @Schema(description = "User unique identifier", example = "42")
    Long id,

    @Schema(description = "User username", example = "maria")
    String username,

    @Schema(description = "User email address, as recorded on the profile; null without a profile",
        example = "maria.lopez@example.com", nullable = true)
    String email,

    @Schema(description = "User role", example = "OWNER_ROLE", allowableValues = {"OWNER_ROLE", "TECHNICIAN_ROLE"})
    String role,

    @Schema(description = "User identity provider", example = "LOCAL", allowableValues = {"LOCAL", "GOOGLE"})
    String provider
) {
}
