package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;

/** REST representation of whether a board is reporting. */
@Schema(
    name = "DeviceConnectivityState",
    description = "Whether a board is reporting, evaluated by the server at read time: REPORTING when its latest "
        + "batch arrived within the offline window of its unit's policy, SILENT when longer ago, NEVER_REPORTED "
        + "when paired but no batch arrived yet, KEY_REVOKED when its API key was withdrawn, UNPAIRED when it is "
        + "linked to no unit",
    example = "REPORTING")
public enum DeviceConnectivityStateResource {
  REPORTING,
  SILENT,
  NEVER_REPORTED,
  KEY_REVOKED,
  UNPAIRED;

  public DeviceConnectivityState toDomain() {
    return DeviceConnectivityState.valueOf(name());
  }

  public static DeviceConnectivityStateResource fromDomain(DeviceConnectivityState state) {
    return valueOf(state.name());
  }
}
