package pe.edu.upc.ice.track.platform.iam.infrastructure.tokens.refresh.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens.RefreshTokenService;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Refresh token service implementation for opaque tokens.
 *
 * <p>Tokens are 256 bits drawn from {@link SecureRandom} and Base64URL encoded. They are persisted
 * as their SHA-256 digest: with that much entropy a plain, unsalted hash cannot be reversed, and
 * unlike bcrypt it stays deterministic, so the digest can be looked up through a unique index.</p>
 */
@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {
  private static final int TOKEN_BYTE_LENGTH = 32;
  private static final String DIGEST_ALGORITHM = "SHA-256";

  private final SecureRandom secureRandom = new SecureRandom();
  private final Base64.Encoder tokenEncoder = Base64.getUrlEncoder().withoutPadding();
  private final Duration timeToLive;

  /**
   * Creates the service.
   *
   * @param expirationDays the lifetime of a refresh token, in days; must be positive
   */
  public RefreshTokenServiceImpl(@Value("${authorization.refresh-token.expiration.days}") long expirationDays) {
    if (expirationDays <= 0) {
      throw new IllegalArgumentException("authorization.refresh-token.expiration.days must be positive");
    }
    this.timeToLive = Duration.ofDays(expirationDays);
  }

  // inherited javadoc
  @Override
  public String generateToken() {
    var bytes = new byte[TOKEN_BYTE_LENGTH];
    secureRandom.nextBytes(bytes);
    return tokenEncoder.encodeToString(bytes);
  }

  // inherited javadoc
  @Override
  public String hashToken(String token) {
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("refresh token must not be null or blank");
    }
    return HexFormat.of().formatHex(newDigest().digest(token.getBytes(StandardCharsets.UTF_8)));
  }

  // inherited javadoc
  @Override
  public Instant calculateExpiryDate(Instant issuedAt) {
    return issuedAt.plus(timeToLive);
  }

  /**
   * Creates a digest instance; {@link MessageDigest} is not thread safe, so one is used per call.
   *
   * @return a SHA-256 message digest
   */
  private static MessageDigest newDigest() {
    try {
      return MessageDigest.getInstance(DIGEST_ALGORITHM);
    } catch (NoSuchAlgorithmException exception) {
      // Every Java platform implementation is required to support SHA-256.
      throw new IllegalStateException("%s is not available".formatted(DIGEST_ALGORITHM), exception);
    }
  }
}
