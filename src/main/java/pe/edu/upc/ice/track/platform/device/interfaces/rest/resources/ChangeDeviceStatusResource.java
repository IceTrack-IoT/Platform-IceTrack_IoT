package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

/** Request for an operator-controlled device lifecycle transition. */
public record ChangeDeviceStatusResource(
    @NotNull(message = "{validation.not-null}")
    DeviceStatusResource newStatus) {
}
