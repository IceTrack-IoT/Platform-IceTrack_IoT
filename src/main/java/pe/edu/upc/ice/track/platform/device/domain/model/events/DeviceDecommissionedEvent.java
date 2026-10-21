package pe.edu.upc.ice.track.platform.device.domain.model.events;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.time.LocalDateTime;

/** Published when a board is permanently retired from the device fleet. */
public record DeviceDecommissionedEvent(Long deviceId, String deviceUid, LocalDateTime decommissionedAt) {
  public static DeviceDecommissionedEvent from(Device device) {
    return new DeviceDecommissionedEvent(device.getDeviceId(), device.getDeviceUid(), LocalDateTime.now());
  }
}
