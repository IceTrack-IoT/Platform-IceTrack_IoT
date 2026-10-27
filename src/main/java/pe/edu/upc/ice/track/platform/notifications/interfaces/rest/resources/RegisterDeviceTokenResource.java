package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;

/** Inbound resource to register the FCM token of the caller's device. */
public record RegisterDeviceTokenResource(
    @NotBlank @Size(max = 512) String token,
    @NotNull DevicePlatform platform) {
}
