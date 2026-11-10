package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource for one entry of an owner's equipment directory.
 *
 * <p>The identity of a unit and nothing else: what another screen needs to name a unit, show its
 * status and say where it is installed. Telemetry, thresholds and the configuration version are left
 * out on purpose. They change with every reading, while the directory is read conditionally: a
 * property that moved all the time would make its entity tag useless.</p>
 */
@Schema(
    name = "EquipmentDirectoryEntryResponse",
    description = "Identity of a refrigeration unit and of the site it is installed at",
    example = "{\"id\": 10, \"uid\": \"EQ-0001\", \"name\": \"Cold Room #01\", \"equipment_type\": \"COLD_ROOM\", "
        + "\"status\": \"ACTIVE\", \"site_id\": 1, \"site_name\": \"Callao Logistics Hub\"}")
public record EquipmentDirectoryEntryResource(
    @Schema(description = "Equipment unique identifier", example = "10")
    Long id,

    @Schema(description = "The unit's own identifier, unique across the platform", example = "EQ-0001")
    String uid,

    @Schema(description = "Name assigned to the unit", example = "Cold Room #01")
    String name,

    @Schema(description = "Kind of refrigeration unit", example = "COLD_ROOM")
    EquipmentTypeResource equipmentType,

    @Schema(description = "Current lifecycle status", example = "ACTIVE")
    StatusEquipmentResource status,

    @Schema(description = "Identifier of the site the unit is installed at", example = "1")
    Long siteId,

    @Schema(description = "Name of that site at the time of the read", example = "Callao Logistics Hub")
    String siteName
) {
}
