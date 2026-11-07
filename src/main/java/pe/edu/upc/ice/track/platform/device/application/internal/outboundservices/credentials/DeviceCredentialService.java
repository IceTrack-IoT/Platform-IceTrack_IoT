package pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials;

import java.util.Optional;

/** Port for issuing and verifying the API key of a physical device. */
public interface DeviceCredentialService {

  /** Generates a new random API key together with its digest. */
  IssuedDeviceCredential issue();

  /**
   * Computes the lookup digest of a presented API key.
   *
   * @param apiKey the key received from the edge
   * @return the hex digest, or empty when the key is missing or not a well-formed device key
   */
  Optional<String> digestOf(String apiKey);

  /**
   * Checks a presented API key against a stored digest in constant time.
   *
   * @param apiKey       the key received from the edge
   * @param storedDigest the hex digest stored for the device
   * @return {@code true} only when both are present and the key hashes to the stored digest
   */
  boolean matches(String apiKey, String storedDigest);
}
