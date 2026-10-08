package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource representing a registered site.
 *
 * <p>Carries the owner identifier because an owner listing has to tell a caller which of the rows
 * is whose, but it is read-only here: it is never accepted as input.</p>
 */
@Schema(
    name = "SiteResponse",
    description = "Site information response",
    example = "{\"id\": 1, \"owner_id\": 42, \"name\": \"Sede Central\", \"address\": \"Av. Primavera 123, Lima\", \"contact_name\": \"Maria Lopez\", \"phone\": \"+51 987654321\"}")
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
    String phone
) {
}