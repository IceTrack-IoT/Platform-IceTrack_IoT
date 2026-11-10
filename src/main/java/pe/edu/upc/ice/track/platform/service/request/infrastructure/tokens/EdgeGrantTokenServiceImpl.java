package pe.edu.upc.ice.track.platform.service.request.infrastructure.tokens;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.tokens.EdgeGrantTokenService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.EdgeGrant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Generates edge grant tokens and protects them with a SHA-256 digest.
 *
 * <p>A token is {@value EdgeGrant#TOKEN_PREFIX} followed by 32 bytes from {@link SecureRandom}, hex
 * encoded (64 characters, 256 bits of entropy). With that much entropy a plain, unsalted SHA-256
 * digest cannot be reversed or brute-forced, which is also what lets the gateway compare a
 * presented token against the digests of its configuration with a single hash. Device API keys
 * and refresh tokens are protected the same way.</p>
 */
@Service
public class EdgeGrantTokenServiceImpl implements EdgeGrantTokenService {

  private static final int SECRET_BYTES = 32;
  private static final String DIGEST_ALGORITHM = "SHA-256";
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();
  private static final HexFormat HEX = HexFormat.of();

  @Override
  public IssuedEdgeGrantToken generate() {
    var randomBytes = new byte[SECRET_BYTES];
    SECURE_RANDOM.nextBytes(randomBytes);
    var plainText = EdgeGrant.TOKEN_PREFIX + HEX.formatHex(randomBytes);
    return new IssuedEdgeGrantToken(plainText, HEX.formatHex(sha256(plainText)));
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
