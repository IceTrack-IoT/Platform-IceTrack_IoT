package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing a registered site.
 *
 * <p>{@code location} and {@code navigation_url} are <em>omitted</em> from the JSON when the site has
 * no coordinates; they are never sent as {@code null}. A client therefore never has to tell real
 * coordinates from placeholders.</p>
 *
 * <p>{@code geocoding_failure_reason} is always present: {@code null} unless {@code geocoding_status}
 * is {@code FAILED} or {@code NOT_FOUND}. {@code version} grows on every change and is the value a
 * client sends back in {@code If-Match} to update the site safely.</p>
 */
@Schema(
    name = "SiteResponse",
    description = "Site information response",
    example = "{\"id\": 1, \"owner_id\": 42, \"name\": \"Sede Central\", \"address\": \"Av. Primavera 123, Lima\", "
        + "\"contact_name\": \"Maria Lopez\", \"phone\": \"+51 987654321\", \"equipment_count\": 3, "
        + "\"geocoding_status\": \"RESOLVED\", \"geocoding_failure_reason\": null, "
        + "\"location\": {\"latitude\": -12.0931, \"longitude\": -77.0465, "
        + "\"place_id\": \"ChIJ8e8c9yLIBZERt0P0wP0xV5A\"}, \"navigation_url\": "
        + "\"https://www.google.com/maps/dir/?api=1&destination=-12.0931,-77.0465&destination_place_id=ChIJ8e8c9yLIBZERt0P0wP0xV5A\", "
        + "\"version\": 3}")
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

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @Schema(description = "Why the site has no location; null unless geocoding_status is FAILED or NOT_FOUND",
        nullable = true, example = "ADDRESS_NOT_FOUND")
    GeocodingFailureReasonResource geocodingFailureReason,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Resolved coordinates; omitted unless geocoding_status is RESOLVED",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    GeoLocationResource location,

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Google Maps directions link; omitted unless the site is located",
        example = "https://www.google.com/maps/dir/?api=1&destination=-12.0931,-77.0465",
        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    String navigationUrl,

    @Schema(description = "Concurrency version; grows on every change. Send it back in If-Match to update the site",
        example = "3")
    Long version
) {
}
