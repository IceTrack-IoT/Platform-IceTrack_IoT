package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

/** REST representation of the lifecycle state of an IoT board. */
@Schema(
    name = "DeviceStatus",
    description = "Lifecycle state of an IoT board: PROVISIONED before its first connection, ACTIVE while "
        + "reporting, SUSPENDED while temporarily blocked, REVOKED when its credentials were withdrawn and "
        + "DECOMMISSIONED when it was retired",
    example = "ACTIVE")
public enum DeviceStatusResource {
  PROVISIONED,
  ACTIVE,
  SUSPENDED,
  REVOKED,
  DECOMMISSIONED;

  public DeviceStatus toDomain() {
    return DeviceStatus.valueOf(name());
  }

  public static DeviceStatusResource fromDomain(DeviceStatus status) {
    return valueOf(status.name());
  }
}
