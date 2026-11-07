package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

/**
 * Command linking a Google identity to an account that was registered with a local password.
 *
 * <p>A Google sign-in whose verified email belongs to a local account is never linked silently,
 * since whoever registered that email locally would keep password access to the account. The
 * account holder must authorize the link by presenting the account's current password together
 * with the Google id_token. The account is resolved from the verified Google email, never from a
 * client supplied identifier.</p>
 *
 * @param idToken  the Google OIDC id_token issued to the frontend; required
 * @param password the current local password of the account to link
 * @param channel  the client application the request comes from; required
 */
public record LinkGoogleAccountCommand(String idToken, String password, ClientChannel channel) {

  /**
   * Validates that a token and a channel were actually supplied.
   */
  public LinkGoogleAccountCommand {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException("Google id_token must not be null or blank");
    }
    if (channel == null) {
      throw new IllegalArgumentException("channel must not be null");
    }
  }
}
