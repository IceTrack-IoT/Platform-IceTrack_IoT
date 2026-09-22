package pe.edu.upc.ice.track.platform.iam.domain.model.commands;

/**
 * Exchange Google token command.
 *
 * <p>Represents the OAuth2 Token Exchange request handled by the IAM bounded context: the
 * frontend performs the Google login, obtains the OIDC {@code id_token} and submits it here.
 * The command service validates the token, signs the matching user in - registering it first
 * when the Google account is unknown - and returns the platform's own bearer token.</p>
 *
 * <p>This command replaces the former {@code SignInByGoogleCommand} and
 * {@code SignUpByGoogleCommand} pair. Sign-in and sign-up are indistinguishable from the
 * caller's point of view in a token exchange, and splitting them forced the interface layer to
 * orchestrate a "try to sign in, otherwise sign up" sequence that belongs to the application
 * layer.</p>
 *
 * @param idToken       the Google OIDC id_token issued to the frontend; required
 * @param requestedRole the platform role to assign when the Google account is registered for the
 *                      first time, expressed as a {@code Roles} name such as {@code OWNER_ROLE}
 *                      or {@code TECHNICIAN_ROLE}. May be {@code null} or blank, in which case
 *                      the default role is assigned. Ignored for already registered accounts.
 */
public record ExchangeGoogleTokenCommand(String idToken, String requestedRole) {

  /**
   * Creates the command without an explicit role, letting the default role apply on registration.
   *
   * @param idToken the Google OIDC id_token issued to the frontend
   */
  public ExchangeGoogleTokenCommand(String idToken) {
    this(idToken, null);
  }

  /**
   * Validates that a token was actually supplied.
   */
  public ExchangeGoogleTokenCommand {
    if (idToken == null || idToken.isBlank()) {
      throw new IllegalArgumentException("Google id_token must not be null or blank");
    }
  }

  /**
   * Indicates whether the caller asked for a specific role at registration time.
   *
   * @return {@code true} when a non-blank role name was supplied
   */
  public boolean hasRequestedRole() {
    return requestedRole != null && !requestedRole.isBlank();
  }
}
