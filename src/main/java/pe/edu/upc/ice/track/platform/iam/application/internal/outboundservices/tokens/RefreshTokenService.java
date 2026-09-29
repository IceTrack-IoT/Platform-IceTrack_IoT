package pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens;

import java.time.Instant;

/**
 * Outbound port for the issuance of opaque refresh tokens used by IAM commands.
 *
 * <p>Refresh tokens are random values, not JWTs: they carry no claims and are only meaningful
 * through the session they are persisted with. Only their digest is persisted.</p>
 */
public interface RefreshTokenService {

  /**
   * Generates a new raw refresh token.
   *
   * @return an unpredictable, URL safe token value
   */
  String generateToken();

  /**
   * Computes the digest under which a raw refresh token is persisted and looked up.
   *
   * @param token raw token value; required
   * @return the deterministic one-way digest of {@code token}
   */
  String hashToken(String token);

  /**
   * Computes the expiry date of a refresh token.
   *
   * @param issuedAt the instant the token is issued
   * @return the instant after which the token is no longer accepted
   */
  Instant calculateExpiryDate(Instant issuedAt);
}
