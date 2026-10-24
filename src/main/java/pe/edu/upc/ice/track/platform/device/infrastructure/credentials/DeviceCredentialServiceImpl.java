package pe.edu.upc.ice.track.platform.device.infrastructure.credentials;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.IssuedDeviceCredential;
import pe.edu.upc.ice.track.platform.iam.infrastructure.hashing.bcrypt.BCryptHashingService;

import java.security.SecureRandom;
import java.util.Base64;

/** Generates high-entropy device API keys and protects them with the platform BCrypt service. */
@Service
public class DeviceCredentialServiceImpl implements DeviceCredentialService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int SECRET_BYTES = 32;
  private static final String PREFIX = "it_dev_";

  private final BCryptHashingService hashingService;

  public DeviceCredentialServiceImpl(BCryptHashingService hashingService) {
    this.hashingService = hashingService;
  }

  @Override
  public IssuedDeviceCredential issue() {
    var randomBytes = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    var plainText = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    return new IssuedDeviceCredential(plainText, hashingService.encode(plainText));
  }

  @Override
  public boolean matches(String apiKey, String storedHash) {
    return apiKey != null && !apiKey.isBlank() && storedHash != null && !storedHash.isBlank()
        && hashingService.matches(apiKey, storedHash);
  }
}
