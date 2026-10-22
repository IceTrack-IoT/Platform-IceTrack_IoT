package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing a registered site.
 *
 * <p>Carries the owner identifier because an owner listing has to tell a caller which of the rows
 * is whose, but it is read-only here: it is never accepted as input.</p>
 *
 * <p>{@code location} and {@code navigation_url} are present only when {@code geocoding_status}
 * is {@code RESOLVED}, and {@code null} otherwise. A client therefore never has to tell real
 * coordinates from placeholder zeroes: no coordinates are sent unless they were resolved.</p>
 */
@Schema(
    name = "SiteResponse",
    description = "Site information response",
    example = "{\"id\": 1, \"owner_id\": 42, \"name\": \"Sede Central\", \"address\": \"Av. Primavera 123, Lima\", "
        + "\"contact_name\": \"Maria Lopez\", \"phone\": \"+51 987654321\", \"equipment_count\": 3, "
        + "\"geocoding_status\": \"RESOLVED\", \"location\": {\"latitude\": -12.0931, \"longitude\": -77.0465, "
        + "\"place_id\": \"ChIJ8e8c9yLIBZERt0P0wP0xV5A\"}, \"navigation_url\": "
        + "\"https://www.google.com/maps/dir/?api=1&destination=-12.0931,-77.0465&destination_place_id=ChIJ8e8c9yLIBZERt0P0wP0xV5A\"}")
public record SiteResource(
    @Schema(description = "Site unique identifier", example = "1")
    Long id,

    @Schema(description = "Identifier of the owner profile the site belongs to", example = "42")
    Long ownerId,

    @Schema(description = "Site name", example = "Sede Central")
    String name,

    @Schema(description = "Physical address of the site", example = "Av. Primavera 123, Lima")
    String address,

    @Schema(description = "Name of the person reachable on site", example = "Maria Lopez")
    String contactName,

    @Schema(description = "Contact phone number", example = "+51 987654321")
    String phone,

    @Schema(description = "Number of equipment units installed at the site", example = "3")
    Integer equipmentCount,

    @Schema(description = "Where the site stands in locating its address", example = "RESOLVED")
    GeocodingStatusResource geocodingStatus,

    @Schema(
        description = "Resolved coordinates; null unless geocoding_status is RESOLVED",
        nullable = true)
    GeoLocationResource location,

    @Schema(
        description = "Google Maps directions link to the site; null unless geocoding_status is RESOLVED",
        example = "https://www.google.com/maps/dir/?api=1&destination=-12.0931,-77.0465",
        nullable = true)
    String navigationUrl
) {
}
