package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetails;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceCredentialResource;

/** Converts a one-time credential issuance result into its REST response. */
public final class DeviceCredentialResourceAssembler {
  private DeviceCredentialResourceAssembler() {
  }

  /**
   * @param result  the issuance result, carrying the plaintext key
   * @param details the device of that result, described with the unit it is linked to
   * @return the response delivering the key once, next to the device
   */
  public static DeviceCredentialResource toResourceFromResult(DeviceCredentialResult result, DeviceDetails details) {
    return new DeviceCredentialResource(
        DeviceResourceFromEntityAssembler.toResourceFromDetails(details), result.apiKey());
  }
}
