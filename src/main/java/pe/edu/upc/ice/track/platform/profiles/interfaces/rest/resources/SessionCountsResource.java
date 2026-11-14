package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * How many sites and units the signed-in owner has, inside a session context.
 *
 * @param sites     the number of sites; {@code 0} for a technician
 * @param equipment the number of units across those sites; {@code 0} for a technician
 */
@Schema(name = "SessionCounts", description = "Sites and units of the signed-in owner; both 0 for a technician",
    example = "{\"sites\": 2, \"equipment\": 7}")
public record SessionCountsResource(
    @Schema(description = "Number of sites of the owner", example = "2")
    long sites,

    @Schema(description = "Number of units across the owner's sites", example = "7")
    long equipment
) {
}
