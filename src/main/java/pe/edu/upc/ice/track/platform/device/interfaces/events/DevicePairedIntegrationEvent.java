package pe.edu.upc.ice.track.platform.device.interfaces.events;

import java.time.LocalDateTime;

/** Published language event emitted after a board is linked to a refrigeration unit. */
public record DevicePairedIntegrationEvent(
    Long deviceId,
    Long equipmentId,
    LocalDateTime pairedAt) {
}
