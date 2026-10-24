package pe.edu.upc.ice.track.platform.device.interfaces.events;

import java.time.LocalDateTime;

/** Published language event emitted when a board is permanently retired. */
public record DeviceDecommissionedIntegrationEvent(
    Long deviceId,
    String deviceUid,
    LocalDateTime decommissionedAt) {
}
