package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command to replace a credential of a device owned by the requesting username. */
public record RotateApiKeyCommand(Long deviceId, String username) {
}
