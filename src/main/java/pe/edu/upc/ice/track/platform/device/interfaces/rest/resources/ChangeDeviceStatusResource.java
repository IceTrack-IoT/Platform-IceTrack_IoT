package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** Request for an operator-controlled device lifecycle transition. */
public record ChangeDeviceStatusResource(
    @JsonProperty("new_status")
    @NotNull(message = "{validation.not-null}")
    DeviceStatusResource newStatus) {
}
