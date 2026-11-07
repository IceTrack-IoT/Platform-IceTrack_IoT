package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

/**
 * Sign in command by local authentication.
 *
 * <p>The account is identified by its username or by the email address of its profile. Every
 * credential failure, whether the account is unknown or the password is wrong, is answered the
 * same way, so the outcome never reveals which accounts exist.</p>
 *
 * @param username the username or email address of the account; required
 * @param password the password of the account
 * @param channel  the client application the sign-in comes from; required
 */
public record SignInByLocalCommand(
    String username,
    String password,
    ClientChannel channel) {

  /**
   * Validates that the identifier and the channel were supplied.
   */
  public SignInByLocalCommand {
    if (username == null || username.isBlank()) {
      throw new IllegalArgumentException("username must not be null or blank");
    }
    if (channel == null) {
      throw new IllegalArgumentException("channel must not be null");
    }
  }
}
