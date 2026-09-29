package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

/**
 * Refresh token command.
 *
 * <p>Exchanges a refresh token for a new session. The presented refresh token is rotated: it is
 * revoked and replaced by a new one, issued together with a new access token. Presenting a
 * refresh token that was already rotated is treated as a replay and revokes every session of its
 * account.</p>
 *
 * @param refreshToken the opaque refresh token issued at sign-in or by a previous refresh; required
 */
public record RefreshTokenCommand(String refreshToken) {

  /**
   * Validates that a token was actually supplied.
   */
  public RefreshTokenCommand {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("refresh_token must not be null or blank");
    }
  }
}
