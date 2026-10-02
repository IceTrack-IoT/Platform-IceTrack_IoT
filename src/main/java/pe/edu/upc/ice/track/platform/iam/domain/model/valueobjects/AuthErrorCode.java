package pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects;

/**
 * Machine-readable catalog of the reasons a refresh token is rejected.
 *
 * <p>The constant names are part of the public API contract: they are returned verbatim as the
 * {@code code} of the error response, so clients can branch on them without parsing messages.
 * Only {@link #REFRESH_TOKEN_RECENTLY_ROTATED} is recoverable: the client should retry once with
 * the latest refresh token it holds. Every other code ends the session.</p>
 */
public enum AuthErrorCode {

  /**
   * The token was rotated moments ago, within the reuse grace period: a concurrent refresh by
   * the legitimate client, such as a second browser tab. Recoverable.
   */
  REFRESH_TOKEN_RECENTLY_ROTATED(
      "The provided refresh token was already rotated within the acceptable grace period window."),

  /**
   * The token was rotated and is presented again after the grace period: it may have been stolen.
   * Every session of the account is revoked.
   */
  REFRESH_TOKEN_REPLAY_DETECTED(
      "The provided refresh token was already used; all sessions of the account have been revoked."),

  /**
   * The token reached the end of its natural lifetime.
   */
  REFRESH_TOKEN_EXPIRED("The provided refresh token has expired."),

  /**
   * The token was explicitly revoked, by a sign-out or by a revocation of every session of the account.
   */
  REFRESH_TOKEN_REVOKED("The provided refresh token has been revoked."),

  /**
   * The token is malformed or matches no session.
   */
  REFRESH_TOKEN_INVALID("The provided refresh token is invalid.");

  private final String defaultMessage;

  AuthErrorCode(String defaultMessage) {
    this.defaultMessage = defaultMessage;
  }

  /**
   * Returns the human-readable description of this code.
   *
   * @return the default message, never {@code null}
   */
  public String getDefaultMessage() {
    return defaultMessage;
  }
}
