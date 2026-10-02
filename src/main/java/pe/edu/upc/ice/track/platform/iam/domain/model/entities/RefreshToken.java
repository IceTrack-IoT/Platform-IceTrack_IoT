package pe.edu.upc.ice.track.platform.iam.domain.model.entities;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

/**
 * Refresh token domain entity.
 *
 * <p>Represents one session of a {@code User}. The raw token handed to the client is never
 * stored: {@code token} holds its one-way digest, so a leaked database cannot be replayed against
 * the refresh endpoint. The account is referenced by identity rather than by object, since the
 * {@code User} aggregate does not own its sessions.</p>
 *
 * <p>Tokens are single use. A token that has been exchanged is {@link #rotate rotated}: it is
 * marked revoked, stamped with the revocation instant and linked to the digest of the token that
 * replaced it, instead of being removed. That keeps the three ways a token ends distinguishable:</p>
 * <ul>
 *   <li><strong>rotated</strong> ({@code replacedByToken} set) - presenting it again moments later
 *       is a concurrent refresh by the legitimate client; later on it is a replay;</li>
 *   <li><strong>revoked</strong> ({@code revoked} without a replacement) - ended by a sign-out or by
 *       the revocation of every session of the account;</li>
 *   <li><strong>expired</strong> - past its {@code expiryDate}.</li>
 * </ul>
 *
 * <p>All instants are UTC {@link Instant}s taken from the server clock, so no time zone enters
 * the comparisons.</p>
 */
@Getter
public class RefreshToken {

  private final Long id;
  private final Long userId;
  private final String token;
  private final Instant expiryDate;
  private boolean revoked;
  private Instant revokedAt;
  private String replacedByToken;

  /**
   * Reconstitutes a refresh token, typically from persistence.
   *
   * <p>New tokens must be created through {@link #issue}.</p>
   *
   * @param id              the persistence identity, or {@code null} for a token not yet persisted
   * @param userId          the identifier of the account the session belongs to; required
   * @param token           the digest of the raw token; required
   * @param expiryDate      the instant after which the token is no longer accepted; required
   * @param revoked         whether the token was already rotated or revoked
   * @param revokedAt       the instant the token was rotated or revoked, or {@code null} while it
   *                        is active or when it was revoked before the instant was recorded
   * @param replacedByToken the digest of the token that replaced this one, or {@code null} when it
   *                        was not rotated
   * @throws IllegalArgumentException when a required value is missing
   */
  public RefreshToken(
      Long id,
      Long userId,
      String token,
      Instant expiryDate,
      boolean revoked,
      Instant revokedAt,
      String replacedByToken) {
    if (userId == null) {
      throw new IllegalArgumentException("userId must not be null");
    }
    this.id = id;
    this.userId = userId;
    this.token = requireText(token, "token");
    if (expiryDate == null) {
      throw new IllegalArgumentException("expiryDate must not be null");
    }
    this.expiryDate = expiryDate;
    this.revoked = revoked;
    this.revokedAt = revokedAt;
    this.replacedByToken = replacedByToken;
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
    return new RefreshToken(null, userId, tokenDigest, expiryDate, false, null, null);
  }

  /**
   * Rotates this token: revokes it now and links it to the token that replaces it.
   *
   * @param newToken the digest of the replacing token; required
   * @throws IllegalArgumentException when {@code newToken} is missing
   * @throws IllegalStateException    when the token is already revoked or rotated
   */
  public void rotate(String newToken) {
    requireText(newToken, "newToken");
    if (revoked) {
      throw new IllegalStateException("A revoked refresh token cannot be rotated");
    }
    this.revoked = true;
    this.revokedAt = Instant.now();
    this.replacedByToken = newToken;
  }

  /**
   * Indicates whether the token was ended by a rotation rather than by an explicit revocation.
   *
   * @return {@code true} when the token is revoked and was replaced by another token
   */
  public boolean isRotated() {
    return revoked && replacedByToken != null;
  }

  /**
   * Indicates whether the token has passed its expiry date.
   *
   * @return {@code true} when the current instant is after {@code expiryDate}
   */
  public boolean isExpired() {
    return Instant.now().isAfter(expiryDate);
  }

  /**
   * Indicates whether the token was revoked no longer than {@code gracePeriod} ago.
   *
   * <p>When the server clocks of two instances disagree, {@code revokedAt} may lie slightly in the
   * future; the elapsed time is then negative and the token counts as within the grace period,
   * which errs on the side of not revoking the account's sessions.</p>
   *
   * @param gracePeriod the accepted delay between a rotation and a concurrent refresh; required
   * @return {@code true} when the token is revoked and its revocation happened within {@code gracePeriod}
   */
  public boolean isWithinGracePeriod(Duration gracePeriod) {
    if (!revoked || revokedAt == null || gracePeriod == null) {
      return false;
    }
    return Duration.between(revokedAt, Instant.now()).compareTo(gracePeriod) <= 0;
  }

  private static String requireText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException("%s must not be null or blank".formatted(field));
    }
    return value;
  }
}
