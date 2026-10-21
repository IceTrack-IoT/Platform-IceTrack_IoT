package pe.edu.upc.ice.track.platform.device.domain.model.commands;

import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;

/** Command to request an operator-controlled device lifecycle transition. */
public record ChangeDeviceStatusCommand(Long deviceId, Long ownerId, DeviceStatus newStatus) {
}
