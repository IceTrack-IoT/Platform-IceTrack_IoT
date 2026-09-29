package pe.edu.upc.ice.track.platform.iam.domain.repositories;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.RefreshToken;

import java.util.Optional;

/**
 * Refresh token repository
 * <p>
 *     This interface represents the repository for the RefreshToken entity. Tokens are always
 *     addressed by their digest, never by the raw value handed to the client.
 * </p>
 */
public interface RefreshTokenRepository {
  /**
   * Find refresh token by digest
   *
   * @param token the digest of the raw token
   * @return an optional containing the refresh token if found, or empty if not found
   */
  Optional<RefreshToken> findByToken(String token);

  /**
   * Save refresh token
   *
   * @param refreshToken the refresh token to save
   * @return the saved refresh token
   */
  RefreshToken save(RefreshToken refreshToken);

  /**
   * Revoke an active refresh token atomically.
   *
   * <p>The check and the update happen in a single statement, so when two requests present the
   * same token concurrently exactly one of them succeeds.</p>
   *
   * @param token the digest of the raw token
   * @return true if this call revoked the token, false if it was already revoked or does not exist
   */
  boolean revoke(String token);

  /**
   * Revoke every active refresh token of an account
   *
   * @param userId the identifier of the account
   */
  void revokeAllByUserId(Long userId);

  /**
   * Delete refresh token by digest
   *
   * @param token the digest of the raw token
   */
  void deleteByToken(String token);
}
