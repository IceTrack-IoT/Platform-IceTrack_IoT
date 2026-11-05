package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Internal request sent by an Edge gateway to validate a board identity. */
public record DeviceAuthenticationRequest(
    @JsonProperty("device_uid")
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 64, message = "{validation.size.max}")
    String deviceUid) {
}
