package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials;

/** Port for issuing and verifying the credential of a physical device. */
public interface DeviceCredentialService {
  IssuedDeviceCredential issue();

  boolean matches(String apiKey, String storedHash);
}
