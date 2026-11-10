package pe.edu.upc.ice.track.platform.device.application.results;

/** Safe projection returned to the Edge after successful device authentication. */
public record DeviceAuthenticationResult(Long deviceId, Long equipmentId) {
}
