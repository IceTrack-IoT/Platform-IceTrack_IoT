package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

/**
 * Sign out command.
 *
 * <p>Ends the session bound to a refresh token by revoking that token without a replacement, so
 * it can no longer be exchanged for access tokens. Access tokens are stateless and remain valid
 * until they expire, which is why their lifetime is kept short.</p>
 *
 * @param refreshToken the opaque refresh token of the session to end; required
 */
public record SignOutCommand(String refreshToken) {

  /**
   * Validates that a token was actually supplied.
   */
  public SignOutCommand {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new IllegalArgumentException("refresh_token must not be null or blank");
    }
  }
}
