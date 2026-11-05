package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request through which an owner registers an ESP32 for one of its refrigeration units. */
@Schema(
    name = "RegisterDeviceRequest",
    description = "Request to register an ESP32 board for one of the owner's refrigeration units",
    example = "{\"equipment_id\": 21, \"device_uid\": \"ESP32-ICE-001\", \"board_model\": \"ESP32-DevKitC\", "
        + "\"firmware_version\": \"1.0.0\"}"
)
public record RegisterDeviceResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Identifier of the equipment the device will be paired with", example = "21")
    Long equipmentId,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    @Schema(description = "Unique identifier of the board", example = "ESP32-ICE-001", maxLength = 64)
    String deviceUid,

    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    @Schema(description = "Board model", example = "ESP32-DevKitC", maxLength = 64)
    String boardModel,

    @Size(max = 64, message = "{validation.size.max}")
    @Schema(description = "Firmware version currently running on the board", example = "1.0.0",
        maxLength = 64, nullable = true)
    String firmwareVersion) {
}
