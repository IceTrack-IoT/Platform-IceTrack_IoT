package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing the resolved coordinates of a site.
 *
 * <p>Only ever built from a resolved location, so a client that receives one can trust both
 * coordinates; a site without coordinates carries no {@code location} at all rather than one
 * filled with zeroes.</p>
 */
@Schema(
    name = "GeoLocationResponse",
    description = "Resolved coordinates of a site",
    example = "{\"latitude\": -12.0931, \"longitude\": -77.0465, \"place_id\": \"ChIJ8e8c9yLIBZERt0P0wP0xV5A\"}")
public record GeoLocationResource(
    @Schema(description = "Latitude in degrees", example = "-12.0931")
    Double latitude,

    @Schema(description = "Longitude in degrees", example = "-77.0465")
    Double longitude,

    @Schema(
        description = "Google Maps place identifier; null when the provider supplied none",
        example = "ChIJ8e8c9yLIBZERt0P0wP0xV5A",
        nullable = true)
    String placeId
) {
}
