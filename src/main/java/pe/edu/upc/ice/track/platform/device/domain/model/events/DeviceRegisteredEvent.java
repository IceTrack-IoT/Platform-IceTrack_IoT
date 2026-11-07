package pe.edu.upc.ice.track.platform.device.domain.model.events;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.time.LocalDateTime;

/** Published once a newly registered board receives its persistence identity. */
public record DeviceRegisteredEvent(Long deviceId, String deviceUid, LocalDateTime registeredAt) {
  public static DeviceRegisteredEvent from(Device device) {
    return new DeviceRegisteredEvent(device.getDeviceId(), device.getDeviceUid(), LocalDateTime.now());
  }
}
