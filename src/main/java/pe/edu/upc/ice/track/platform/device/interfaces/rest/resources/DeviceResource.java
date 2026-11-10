package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Safe REST representation of a managed device; it intentionally omits the API key digest.
 *
 * <p>{@code paired_equipment_name} and {@code paired_site_id} are read-only companions of
 * {@code paired_equipment_id}: they describe the unit at the time of the read, so a device list can
 * be shown without also reading the equipment directory.</p>
 */
@Schema(
    name = "DeviceResponse",
    description = "Managed IoT device; the API key digest is never exposed",
    example = "{\"id\": 7, \"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", "
        + "\"firmware_version\": \"1.0.0\", \"status\": \"PAIRED\", \"paired_equipment_id\": 21, "
        + "\"paired_equipment_name\": \"Cold Room #01\", \"paired_site_id\": 1, "
        + "\"last_seen_at\": \"2026-10-23T17:00:00Z\"}"
)
public record DeviceResource(
    @Schema(description = "Device unique identifier", example = "7")
    Long id,

    @Schema(description = "Unique identifier of the board", example = "ESP32-ICE-001")
    String deviceUid,

    @Schema(description = "Board model", example = "ESP32-DevKitC")
    String boardModel,

    @Schema(description = "Firmware version currently running on the board", example = "1.0.0")
    String firmwareVersion,

    @Schema(description = "Current lifecycle status of the device", example = "PAIRED")
    DeviceStatusResource status,

    @Schema(description = "Identifier of the equipment the device is paired with", example = "21",
        nullable = true)
    Long pairedEquipmentId,

    @Schema(description = "Name of the equipment the device is paired with; null when unpaired",
        example = "Cold Room #01", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    String pairedEquipmentName,

    @Schema(description = "Identifier of the site that equipment is installed at; null when unpaired",
        example = "1", nullable = true, accessMode = Schema.AccessMode.READ_ONLY)
    Long pairedSiteId,

    @Schema(description = "When the platform received the device's latest reading batch; null before the first one",
        example = "2026-10-23T22:00:00Z", nullable = true)
    Instant lastSeenAt) {
}
