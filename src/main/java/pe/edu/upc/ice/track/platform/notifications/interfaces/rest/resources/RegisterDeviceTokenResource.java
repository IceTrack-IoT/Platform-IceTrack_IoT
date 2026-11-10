package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;

/** Inbound resource to register the FCM token of the caller's device. */
@Schema(
    name = "RegisterDeviceTokenRequest",
    description = "Request to register the FCM token of the caller's device",
    example = "{\"token\": \"fcm-token-abc123\", \"platform\": \"ANDROID\"}"
)
public record RegisterDeviceTokenResource(
    @NotBlank(message = "{validation.not-blank}")
    @Size(max = 512, message = "{validation.size.max}")
    @Schema(description = "FCM registration token of the device", example = "fcm-token-abc123",
        maxLength = 512)
    String token,

    @NotNull(message = "{validation.not-null}")
    @Schema(description = "Operating system of the device", example = "ANDROID",
        allowableValues = {"ANDROID", "IOS", "WEB"})
    DevicePlatform platform) {
}
