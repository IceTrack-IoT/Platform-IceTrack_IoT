package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceStatusResource;

import java.time.format.DateTimeFormatter;

/** Converts a Device aggregate into its safe public REST representation. */
public final class DeviceResourceFromEntityAssembler {
  private DeviceResourceFromEntityAssembler() {
  }

  public static DeviceResource toResourceFromEntity(Device device) {
    return new DeviceResource(
        device.getDeviceId(),
        device.getDeviceUid(),
        device.getBoardModel(),
        device.getFirmwareVersion(),
        DeviceStatusResource.fromDomain(device.getStatus()),
        device.getPairedEquipmentId(),
        device.getLastSeenAt() == null ? null : device.getLastSeenAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
  }
}
