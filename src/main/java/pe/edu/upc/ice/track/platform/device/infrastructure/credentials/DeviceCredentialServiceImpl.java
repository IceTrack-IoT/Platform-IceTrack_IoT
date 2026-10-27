package pe.edu.upc.ice.track.platform.device.infrastructure.credentials;

import org.springframework.stereotype.Service;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.IssuedDeviceCredential;

import java.security.SecureRandom;
import java.util.Base64;

/** Generates high-entropy device API keys and protects them with BCrypt. */
@Service
public class DeviceCredentialServiceImpl implements DeviceCredentialService {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final int SECRET_BYTES = 32;
  private static final String PREFIX = "it_dev_";

  private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Override
  public IssuedDeviceCredential issue() {
    var randomBytes = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    var plainText = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    return new IssuedDeviceCredential(plainText, passwordEncoder.encode(plainText));
  }

  @Override
  public boolean matches(String apiKey, String storedHash) {
    return apiKey != null && !apiKey.isBlank() && storedHash != null && !storedHash.isBlank()
        && passwordEncoder.matches(apiKey, storedHash);
  }
}
