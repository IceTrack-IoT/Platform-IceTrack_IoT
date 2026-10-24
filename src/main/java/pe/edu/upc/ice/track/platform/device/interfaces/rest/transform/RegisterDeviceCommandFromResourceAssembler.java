package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.device.domain.model.commands.RegisterDeviceCommand;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.RegisterDeviceResource;

/** Converts the registration resource at the REST boundary into its domain command. */
public final class RegisterDeviceCommandFromResourceAssembler {
  private RegisterDeviceCommandFromResourceAssembler() {
  }

  public static RegisterDeviceCommand toCommandFromResource(RegisterDeviceResource resource) {
    return new RegisterDeviceCommand(resource.deviceUid(), resource.boardModel(), resource.firmwareVersion());
  }
}
