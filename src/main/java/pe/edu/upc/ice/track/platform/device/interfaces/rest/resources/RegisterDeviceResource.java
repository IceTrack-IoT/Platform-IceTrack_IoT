package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request used to provision a physical ESP32 board before it is paired. */
@Schema(name = "RegisterDeviceRequest", example = "{\"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", \"firmware_version\": \"1.0.0\"}")
public record RegisterDeviceResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    String deviceUid,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    String boardModel,

    @Size(max = 64, message = "{validation.size.max}")
    String firmwareVersion) {
}
