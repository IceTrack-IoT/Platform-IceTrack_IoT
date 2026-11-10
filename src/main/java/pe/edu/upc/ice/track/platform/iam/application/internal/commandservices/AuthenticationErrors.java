package pe.edu.upc.ice.track.platform.iam.application.internal.commandservices;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;

import java.util.Locale;

/**
 * Catalog of the failures the authentication flows answer with.
 *
 * <p>The codes are part of the public API contract and are mapped onto HTTP statuses by
 * {@code ErrorResponseAssembler}:</p>
 * <ul>
 *   <li>{@code BAD_CREDENTIALS} (401) - unknown account or wrong password, never told apart;</li>
 *   <li>{@code INVALID_TOKEN} (401) - a Google id_token rejected for any reason;</li>
 *   <li>{@code ROLE_CHANNEL_MISMATCH} (403) - valid credentials on the wrong client application;</li>
 *   <li>{@code ACCOUNT_LINK_REQUIRED} (409) - the verified Google email belongs to a local account
 *       whose holder has not authorized the link yet;</li>
 *   <li>{@code GOOGLE_ACCOUNT_NOT_FOUND} (404) - the Google account must complete the onboarding.</li>
 * </ul>
 */
final class AuthenticationErrors {

  static final String BAD_CREDENTIALS_MESSAGE = "Invalid username, email, or password";
  static final String INVALID_GOOGLE_TOKEN_MESSAGE = "Unable to authenticate with Google";

  private AuthenticationErrors() {
  }

  /**
   * @return the single failure every rejected local credential is answered with
   */
  static ApplicationError badCredentials() {
    return new ApplicationError("BAD_CREDENTIALS", "Invalid credentials", BAD_CREDENTIALS_MESSAGE);
  }

  /**
   * @return the failure every rejected Google id_token is answered with
   */
  static ApplicationError invalidGoogleToken() {
    return new ApplicationError("INVALID_TOKEN", "Invalid token", INVALID_GOOGLE_TOKEN_MESSAGE);
  }

  /**
   * @param requiredChannel the channel the account must use instead
   * @return the failure telling the caller which application the account must sign in through
   */
  static ApplicationError roleChannelMismatch(ClientChannel requiredChannel) {
    return new ApplicationError(
        "ROLE_CHANNEL_MISMATCH",
        "Role not allowed on this application",
        "This account must sign in through the %s application"
            .formatted(requiredChannel.name().toLowerCase(Locale.ROOT)));
  }

  /**
   * @return the failure telling the caller that the Google identity must be linked explicitly
   */
  static ApplicationError accountLinkRequired() {
    return new ApplicationError(
        "ACCOUNT_LINK_REQUIRED",
        "Account link required",
        "An account registered with a password already uses this email; confirm its password through "
            + "POST /api/v1/authentication/google/link to link your Google account");
  }

  /**
   * Builds the failure telling the caller that the Google account must complete the onboarding.
   *
   * <p>The code ends with {@code _NOT_FOUND} so it is rendered as a 404, while remaining distinct
   * from a generic missing user for the frontend.</p>
   *
   * @return the onboarding required failure
   */
  static ApplicationError googleAccountNotFound() {
    return new ApplicationError(
        "GOOGLE_ACCOUNT_NOT_FOUND",
        "Google account not registered",
        "Complete the onboarding through POST /api/v1/authentication/google/complete-registration/owner "
            + "or /api/v1/authentication/google/complete-registration/technician");
  }
}
