package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

/** REST representation of the lifecycle state of an IoT board. */
@Schema(
    name = "DeviceStatus",
    description = "Lifecycle state of an IoT board: UNPAIRED when it is linked to no equipment and "
        + "holds no credential, PAIRED while it is linked to an equipment and may submit telemetry "
        + "with its API key, REVOKED when its credential was withdrawn but it is still linked to "
        + "the equipment",
    example = "PAIRED")
public enum DeviceStatusResource {
  UNPAIRED,
  PAIRED,
  REVOKED;

  public DeviceStatus toDomain() {
    return DeviceStatus.valueOf(name());
  }

  public static DeviceStatusResource fromDomain(DeviceStatus status) {
    return valueOf(status.name());
  }
}
