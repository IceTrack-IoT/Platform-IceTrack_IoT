package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response emitted only at pairing or credential rotation, when the plaintext key is delivered once. */
@Schema(name = "DeviceCredentialResponse")
public record DeviceCredentialResource(DeviceResource device, String apiKey) {
}
