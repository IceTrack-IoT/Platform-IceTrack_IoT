package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command to remove the equipment relationship of a device owned by the requesting username. */
public record UnpairDeviceCommand(Long deviceId, String username) {
}
