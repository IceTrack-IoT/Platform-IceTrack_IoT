package pe.edu.upc.ice.track.platform.device.domain.model.events;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.time.LocalDateTime;

/** Published after a board is unlinked from its refrigeration unit and its credential invalidated. */
public record DeviceUnpairedEvent(Long deviceId, Long equipmentId, LocalDateTime unpairedAt) {
  public static DeviceUnpairedEvent from(Device device, Long previousEquipmentId) {
    return new DeviceUnpairedEvent(device.getDeviceId(), previousEquipmentId, LocalDateTime.now());
  }
}
