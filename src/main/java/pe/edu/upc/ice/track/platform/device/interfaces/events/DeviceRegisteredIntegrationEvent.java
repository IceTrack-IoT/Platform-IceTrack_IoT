package pe.edu.upc.ice.track.platform.device.interfaces.events;

import java.time.LocalDateTime;

/** Published language event emitted when Device Management provisions a new board. */
public record DeviceRegisteredIntegrationEvent(
    Long deviceId,
    String deviceUid,
    LocalDateTime registeredAt) {
}
