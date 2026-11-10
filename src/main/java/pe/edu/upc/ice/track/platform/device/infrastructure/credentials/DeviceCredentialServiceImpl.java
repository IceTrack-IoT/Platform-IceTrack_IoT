package pe.edu.upc.ice.track.platform.device.infrastructure.credentials;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.IssuedDeviceCredential;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Generates high-entropy device API keys and protects them with a SHA-256 digest.
 *
 * <p>A key is {@value #PREFIX} followed by 32 bytes from {@link SecureRandom}, hex encoded (64
 * characters, 256 bits of entropy). With that much entropy a plain, unsalted SHA-256 digest cannot
 * be reversed or brute-forced, so the slow, salted hashing that protects low-entropy passwords is
 * unnecessary. A fast digest also lets the edge filter find the device by an indexed lookup on
 * every request instead of spending tens of milliseconds of CPU per verification, a cost an
 * unauthenticated caller could otherwise trigger at will. Refresh tokens are stored the same
 * way.</p>
 */
@Service
public class DeviceCredentialServiceImpl implements DeviceCredentialService {

  static final String PREFIX = "it_dev_";
  private static final int SECRET_BYTES = 32;
  private static final String DIGEST_ALGORITHM = "SHA-256";
  private static final Pattern API_KEY_FORMAT = Pattern.compile(PREFIX + "[0-9a-f]{" + SECRET_BYTES * 2 + "}");
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final HexFormat HEX = HexFormat.of();

  @Override
  public IssuedDeviceCredential issue() {
    var randomBytes = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    var plainText = PREFIX + HEX.formatHex(randomBytes);
    return new IssuedDeviceCredential(plainText, HEX.formatHex(sha256(plainText)));
  }

  @Override
  public Optional<String> digestOf(String apiKey) {
    if (!isWellFormed(apiKey)) {
      return Optional.empty();
    }
    return Optional.of(HEX.formatHex(sha256(apiKey)));
  }

  @Override
  public boolean matches(String apiKey, String storedDigest) {
    if (!isWellFormed(apiKey) || storedDigest == null || storedDigest.isBlank()) {
      return false;
    }
    final byte[] expected;
    try {
      expected = HEX.parseHex(storedDigest);
    } catch (IllegalArgumentException e) {
      return false;
    }
    return MessageDigest.isEqual(sha256(apiKey), expected);
  }

  private static boolean isWellFormed(String apiKey) {
    return apiKey != null && API_KEY_FORMAT.matcher(apiKey).matches();
  }

  private static byte[] sha256(String value) {
    try {
      return MessageDigest.getInstance(DIGEST_ALGORITHM).digest(value.getBytes(StandardCharsets.UTF_8));
    } catch (NoSuchAlgorithmException e) {
      // Every Java platform implementation is required to support SHA-256.
      throw new IllegalStateException(DIGEST_ALGORITHM + " is not available", e);
    }
  }
}
