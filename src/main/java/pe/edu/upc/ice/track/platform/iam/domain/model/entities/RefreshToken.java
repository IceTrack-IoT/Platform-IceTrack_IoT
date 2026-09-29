package pe.edu.upc.ice.track.platform.iam.domain.model.entities;

import lombok.Getter;

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
 * removed, so that presenting it again can be recognized as a replay. The revocation instant is
 * recorded so that a token presented again only moments after its rotation can be told apart
 * from a replay: that is a concurrent refresh by the legitimate client, such as two browser tabs.</p>
 */
@Getter
public class RefreshToken {

  private final Long id;
  private final Long userId;
  private final String token;
  private final Instant expiryDate;
  private final boolean revoked;
  private final Instant revokedAt;

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
   * @param revokedAt  the instant the token was revoked, or {@code null} while it is active or
   *                   when it was revoked before the instant was recorded
   * @throws IllegalArgumentException when a required value is missing
   */
  public RefreshToken(Long id, Long userId, String token, Instant expiryDate, boolean revoked, Instant revokedAt) {
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
    if (token == null || token.isBlank()) {
      throw new IllegalArgumentException("token must not be null or blank");
    }
    if (expiryDate == null) {
      throw new IllegalArgumentException("expiryDate must not be null");
    }
    this.id = id;
    this.userId = userId;
    this.token = token;
    this.expiryDate = expiryDate;
    this.revoked = revoked;
    this.revokedAt = revokedAt;
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
    return new RefreshToken(null, userId, tokenDigest, expiryDate, false, null);
  }

  /**
   * Indicates whether the token was revoked after a given instant.
   *
   * @param instant the reference instant
   * @return {@code true} when the token is revoked and its revocation happened after {@code instant}
   */
  public boolean isRevokedAfter(Instant instant) {
    return revoked && revokedAt != null && revokedAt.isAfter(instant);
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
