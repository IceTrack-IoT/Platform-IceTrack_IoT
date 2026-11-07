package pe.edu.upc.ice.track.platform.device.interfaces.events;

import java.time.LocalDateTime;

/** Published language event emitted after a board is unlinked from a refrigeration unit. */
public record DeviceUnpairedIntegrationEvent(
    Long deviceId,
    Long equipmentId,
    LocalDateTime unpairedAt) {
}
