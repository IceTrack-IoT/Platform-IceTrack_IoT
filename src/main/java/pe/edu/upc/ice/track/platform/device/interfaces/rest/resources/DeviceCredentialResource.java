package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response emitted only at registration or credential rotation, when the plaintext key is delivered once. */
@Schema(
    name = "DeviceCredentialResponse",
    description = "Device data together with the plaintext API key, delivered only once at registration "
        + "or credential rotation",
    example = "{\"device\": {\"id\": 7, \"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", "
        + "\"firmware_version\": \"1.0.0\", \"status\": \"PROVISIONED\", \"paired_equipment_id\": 21, "
        + "\"last_seen_at\": null}, \"api_key\": \"itk_7f3c9a2b4d6e8f0a1b2c3d4e5f6a7b8c\"}"
)
public record DeviceCredentialResource(
    @Schema(description = "Registered device")
    DeviceResource device,

    @Schema(description = "Plaintext API key; store it securely, it is never returned again",
        example = "itk_7f3c9a2b4d6e8f0a1b2c3d4e5f6a7b8c")
    String apiKey) {
}
