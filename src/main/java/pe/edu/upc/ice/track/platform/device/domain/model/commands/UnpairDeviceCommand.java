package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command to remove the equipment relationship of a device. */
public record UnpairDeviceCommand(Long deviceId, Long ownerId) {
}
