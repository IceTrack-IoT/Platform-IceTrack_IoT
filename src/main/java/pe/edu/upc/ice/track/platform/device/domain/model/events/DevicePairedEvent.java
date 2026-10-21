package pe.edu.upc.ice.track.platform.device.domain.model.events;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.time.LocalDateTime;

/** Published after a board is associated with a refrigeration unit. */
public record DevicePairedEvent(Long deviceId, Long equipmentId, LocalDateTime pairedAt) {
  public static DevicePairedEvent from(Device device) {
    return new DevicePairedEvent(device.getDeviceId(), device.getPairedEquipmentId(), LocalDateTime.now());
  }
}
