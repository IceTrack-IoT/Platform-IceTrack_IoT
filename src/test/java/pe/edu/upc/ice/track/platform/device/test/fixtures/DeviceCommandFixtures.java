package pe.edu.upc.ice.track.platform.device.test.fixtures;

import pe.edu.upc.ice.track.platform.device.domain.model.commands.ChangeDeviceStatusCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RegisterDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RotateApiKeyCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.UnpairDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDeviceByIdQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.queries.GetDevicesQuery;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

import java.util.Set;

import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

/**
 *  Fixtures for device commands and queries.
 */
public final class DeviceCommandFixtures {

  private DeviceCommandFixtures() {
    throw new AssertionError("DeviceCommandFixtures is a utility class and must not be instantiated.");
  }

  public static RegisterDeviceCommand registerDeviceCommand() {
    return registerDeviceCommand(EQUIPMENT_ID, VALID_DEVICE_UID, VALID_BOARD_MODEL, VALID_FIRMWARE_VERSION);
  }

  public static RegisterDeviceCommand registerDeviceCommand(
      Long equipmentId, String deviceUid, String boardModel, String firmwareVersion) {
    return new RegisterDeviceCommand(USERNAME, equipmentId, deviceUid, boardModel, firmwareVersion);
  }

  public static UnpairDeviceCommand unpairDeviceCommand() {
    return new UnpairDeviceCommand(DEVICE_ID, USERNAME);
  }

  public static RotateApiKeyCommand rotateApiKeyCommand() {
    return new RotateApiKeyCommand(DEVICE_ID, USERNAME);
  }

  public static ChangeDeviceStatusCommand changeDeviceStatusCommand(DeviceStatus newStatus) {
    return new ChangeDeviceStatusCommand(DEVICE_ID, USERNAME, newStatus);
  }

  public static GetDeviceByIdQuery deviceByIdQuery() {
    return new GetDeviceByIdQuery(DEVICE_ID, USERNAME);
  }

  public static GetDevicesQuery devicesQuery() {
    return new GetDevicesQuery(USERNAME, null, null, null, null, null, null);
  }

  public static GetDevicesQuery devicesQuery(Long equipmentId, Long siteId) {
    return new GetDevicesQuery(USERNAME, equipmentId, siteId, null, null, null, null);
  }

  public static GetDevicesQuery pagedDevicesQuery(Set<DeviceStatus> statuses, String search, int page, int size) {
    return new GetDevicesQuery(USERNAME, null, null, statuses, search, page, size);
  }

  public static GetDevicesQuery devicesQueryByConnectivity(
      Set<DeviceConnectivityState> connectivity, Integer page, Integer size) {
    return new GetDevicesQuery(USERNAME, null, null, null, null, page, size, connectivity);
  }
}
