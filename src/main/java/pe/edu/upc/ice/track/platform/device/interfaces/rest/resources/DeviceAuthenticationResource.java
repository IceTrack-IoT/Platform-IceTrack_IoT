package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

/** Minimal response used by an Edge gateway after successful board authentication. */
public record DeviceAuthenticationResource(boolean authenticated, Long deviceId, Long equipmentId) {
}
