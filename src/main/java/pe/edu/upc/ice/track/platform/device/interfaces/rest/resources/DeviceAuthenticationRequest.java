package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Internal request sent by an Edge gateway to validate a board identity. */
@Schema(
    name = "DeviceAuthenticationRequest",
    description = "Internal request sent by an Edge gateway to validate a board identity",
    example = "{\"device_uid\": \"ESP32-ICE-001\"}"
)
public record DeviceAuthenticationRequest(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    @Schema(description = "Unique identifier of the board", example = "ESP32-ICE-001", maxLength = 64)
    String deviceUid) {
}
