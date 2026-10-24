package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceCredentialResource;

/** Converts a one-time credential issuance result into its REST response. */
public final class DeviceCredentialResourceAssembler {
  private DeviceCredentialResourceAssembler() {
  }

  public static DeviceCredentialResource toResourceFromResult(DeviceCredentialResult result) {
    return new DeviceCredentialResource(
        DeviceResourceFromEntityAssembler.toResourceFromEntity(result.device()), result.apiKey());
  }
}
