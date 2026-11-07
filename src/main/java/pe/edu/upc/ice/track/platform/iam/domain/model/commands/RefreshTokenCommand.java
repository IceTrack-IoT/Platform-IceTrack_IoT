package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

/**
 * Refresh token command.
 *
 * <p>Exchanges a refresh token for a new session. The presented refresh token is rotated: it is
 * revoked and replaced by a new one, issued together with a new access token. Presenting a
 * refresh token that was already rotated is treated as a replay and revokes every session of its
 * account.</p>
 *
 * <p>The new session keeps the channel the original session was opened from. When the client
 * also states its channel, it must be that same channel.</p>
 *
 * @param refreshToken the opaque refresh token issued at sign-in or by a previous refresh; required
 * @param channel      the client application the request comes from, or {@code null} when the
 *                     client does not state it
 */
public record RefreshTokenCommand(String refreshToken, ClientChannel channel) {

  /**
   * Validates that a token was actually supplied.
   */
  public RefreshTokenCommand {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("refresh_token must not be null or blank");
    }
  }
}
