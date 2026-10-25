package pe.edu.upc.ice.track.platform.device.domain.model.commands;

import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

/** Command to request a lifecycle transition for a device owned by the requesting username. */
public record ChangeDeviceStatusCommand(Long deviceId, String username, DeviceStatus newStatus) {
}
