package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Minimal response used by an Edge gateway after successful board authentication. */
@Schema(
    name = "DeviceAuthenticationResponse",
    description = "Minimal response used by an Edge gateway after successful board authentication",
    example = "{\"authenticated\": true, \"device_id\": 7, \"equipment_id\": 21}"
)
public record DeviceAuthenticationResource(
    @Schema(description = "Whether the board credentials were accepted", example = "true")
    boolean authenticated,

    @Schema(description = "Identifier of the authenticated device", example = "7")
    Long deviceId,

    @Schema(description = "Identifier of the equipment the device is paired with", example = "21")
    Long equipmentId) {
}
