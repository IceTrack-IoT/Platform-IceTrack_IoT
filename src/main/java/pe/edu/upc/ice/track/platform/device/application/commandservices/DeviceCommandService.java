package pe.edu.upc.ice.track.platform.device.application.commandservices;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.ChangeDeviceStatusCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.PairDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RegisterDeviceCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.RotateApiKeyCommand;
import pe.edu.upc.ice.track.platform.device.domain.model.commands.UnpairDeviceCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.LocalDateTime;

/** Application contract for operations that change a managed device. */
public interface DeviceCommandService {
  Result<Device, ApplicationError> handle(RegisterDeviceCommand command);

  Result<DeviceCredentialResult, ApplicationError> handle(PairDeviceCommand command);

  Result<Device, ApplicationError> handle(UnpairDeviceCommand command);

  Result<DeviceCredentialResult, ApplicationError> handle(RotateApiKeyCommand command);

  Result<Device, ApplicationError> handle(ChangeDeviceStatusCommand command);

  void recordActivity(Long deviceId, LocalDateTime seenAt);
}
