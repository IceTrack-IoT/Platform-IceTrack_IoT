package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Safe REST representation of a managed device; it intentionally omits the API-key hash. */
@Schema(
    name = "DeviceResponse",
    description = "Managed IoT device; the API-key hash is never exposed",
    example = "{\"id\": 7, \"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", "
        + "\"firmware_version\": \"1.0.0\", \"status\": \"ACTIVE\", \"paired_equipment_id\": 21, "
        + "\"last_seen_at\": \"2026-10-23T17:00:00\"}"
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

    @Schema(description = "Current lifecycle status of the device", example = "ACTIVE")
    DeviceStatusResource status,

    @Schema(description = "Identifier of the equipment the device is paired with", example = "21",
        nullable = true)
    Long pairedEquipmentId,

    @Schema(description = "Last time the device contacted the platform; null before the first contact",
        example = "2026-10-23T17:00:00", nullable = true)
    String lastSeenAt) {
}
