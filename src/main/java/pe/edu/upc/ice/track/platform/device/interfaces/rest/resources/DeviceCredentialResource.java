package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response emitted only at pairing or credential rotation, when the plaintext key is delivered once. */
@Schema(
    name = "DeviceCredentialResponse",
    description = "Device data together with the plaintext API key, delivered only once at pairing "
        + "or credential rotation. The edge sends it in the X-API-Key header.",
    example = "{\"device\": {\"id\": 7, \"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", "
        + "\"firmware_version\": \"1.0.0\", \"status\": \"PAIRED\", \"paired_equipment_id\": 21, "
        + "\"last_seen_at\": null}, "
        + "\"api_key\": \"it_dev_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08\"}"
)
public record DeviceCredentialResource(
    @Schema(description = "Paired device")
    DeviceResource device,

    @Schema(description = "Plaintext API key: it_dev_ followed by 64 hexadecimal characters. Store it "
        + "securely on the board; it is never returned again",
        example = "it_dev_9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08")
    String apiKey) {
}
