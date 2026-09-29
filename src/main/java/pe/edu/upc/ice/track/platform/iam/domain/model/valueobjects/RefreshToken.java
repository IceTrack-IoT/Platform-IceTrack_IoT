package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

import java.time.Instant;

/**
 * Refresh token domain entity.
 *
 * <p>Represents one session of a {@code User}. The raw token handed to the client is never
 * stored: {@code token} holds its one-way digest, so a leaked database cannot be replayed against
 * the refresh endpoint. The account is referenced by identity rather than by object, since the
 * {@code User} aggregate does not own its sessions.</p>
 *
 * <p>Tokens are single use. A token that has been exchanged is marked revoked instead of being
 * removed, so that presenting it again can be recognized as a replay.</p>
 */
public record RefreshToken(Long id, Long userId, String token, Instant expiryDate, boolean revoked) {

  /**
   * Reconstitutes a refresh token, typically from persistence.
   *
   * <p>New tokens must be created through {@link #issue}.</p>
   *
   * @param id         the persistence identity, or {@code null} for a token not yet persisted
   * @param userId     the identifier of the account the session belongs to; required
   * @param token      the digest of the raw token; required
   * @param expiryDate the instant after which the token is no longer accepted; required
   * @param revoked    whether the token was already exchanged or revoked
   * @throws IllegalArgumentException when a required value is missing
   */
  public RefreshToken {
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("token must not be null or blank");
    }
    if (expiryDate == null) {
      throw new IllegalArgumentException("expiryDate must not be null");
    }
  }

  /**
   * Issues a new, active refresh token for an account.
   *
   * @param userId      the identifier of the persisted account; required
   * @param tokenDigest the digest of the raw token handed to the client; required
   * @param expiryDate  the instant after which the token is no longer accepted; required
   * @return the newly built refresh token, not yet persisted
   * @throws IllegalArgumentException when a required value is missing
   */
  public static RefreshToken issue(Long userId, String tokenDigest, Instant expiryDate) {
    return new RefreshToken(null, userId, tokenDigest, expiryDate, false);
  }

  /**
   * Indicates whether the token has reached its expiry date.
   *
   * @param now the reference instant
   * @return {@code true} when the token is no longer valid at {@code now}
   */
  public boolean isExpiredAt(Instant now) {
    return !expiryDate.isAfter(now);
  }
}
