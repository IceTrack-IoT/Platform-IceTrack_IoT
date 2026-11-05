package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/** Request for an operator-controlled device lifecycle transition. */
@Schema(
    name = "ChangeDeviceStatusRequest",
    description = "Request for an operator-controlled device lifecycle transition",
    example = "{\"new_status\": \"SUSPENDED\"}"
)
public record ChangeDeviceStatusResource(
    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Status to move the device to", example = "SUSPENDED",
        allowableValues = {"PROVISIONED", "ACTIVE", "SUSPENDED", "REVOKED", "DECOMMISSIONED"})
    DeviceStatusResource newStatus) {
}
