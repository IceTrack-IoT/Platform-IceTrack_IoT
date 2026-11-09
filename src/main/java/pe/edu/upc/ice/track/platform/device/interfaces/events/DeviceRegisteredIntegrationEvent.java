package pe.edu.upc.ice.track.platform.device.interfaces.events;

import java.time.Instant;

/** Published language event emitted when Device Management provisions a new board. */
public record DeviceRegisteredIntegrationEvent(
    Long deviceId,
    String deviceUid,
    Instant registeredAt) {
}
