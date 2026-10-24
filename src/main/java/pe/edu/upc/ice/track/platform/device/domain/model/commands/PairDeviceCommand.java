package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command to pair a device to an equipment owned by the requesting owner. */
public record PairDeviceCommand(Long deviceId, Long equipmentId, Long ownerId) {
}
