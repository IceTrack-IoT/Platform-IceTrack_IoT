package pe.edu.upc.ice.track.platform.device.domain.model.commands;

/** Command to provision a physical IoT board in the platform fleet. */
public record RegisterDeviceCommand(String deviceUid, String boardModel, String firmwareVersion) {
}
