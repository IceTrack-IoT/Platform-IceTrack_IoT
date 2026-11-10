package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;

/**
 * Sign in command by Google federated authentication.
 *
 * <p>First step of the deferred registration flow: the frontend performs the Google login and
 * submits the resulting OIDC {@code id_token}. When the Google account already has a platform
 * account the user is signed in; otherwise nothing is written and the caller is told to complete
 * the onboarding through a {@link CompleteGoogleOwnerRegistrationCommand} or a
 * {@link CompleteGoogleTechnicianRegistrationCommand}. An account registered locally with the same
 * email is never linked here: linking requires a {@link LinkGoogleAccountCommand}.</p>
 *
 * @param idToken the Google OIDC id_token issued to the frontend; required
 * @param channel the client application the sign-in comes from; required
 */
public record SignInByGoogleCommand(String idToken, ClientChannel channel) {

  /**
   * Validates that a token and a channel were actually supplied.
   */
  public SignInByGoogleCommand {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException("Google id_token must not be null or blank");
    }
    if (channel == null) {
      throw new IllegalArgumentException("channel must not be null");
    }
  }
}
