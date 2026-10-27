package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request through which an owner registers an ESP32 for one of its refrigeration units. */
@Schema(name = "RegisterDeviceRequest", example = "{\"equipment_id\": 21, \"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", \"firmware_version\": \"1.0.0\"}")
public record RegisterDeviceResource(
    @NotNull(message = "{validation.not-null}")
    Long equipmentId,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    String deviceUid,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    String boardModel,

    @Size(max = 64, message = "{validation.size.max}")
    String firmwareVersion) {
}
