package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command to replace a device credential. */
public record RotateApiKeyCommand(Long deviceId, Long ownerId) {
}
