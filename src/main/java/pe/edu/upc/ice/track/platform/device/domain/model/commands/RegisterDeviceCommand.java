package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command through which an owner registers a board for one of its refrigeration units. */
public record RegisterDeviceCommand(
    String username,
    Long equipmentId,
    String deviceUid,
    String boardModel,
    String firmwareVersion) {
}
