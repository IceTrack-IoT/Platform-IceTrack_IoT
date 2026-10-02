package pe.edu.upc.ice.track.platform.iam.domain.exceptions;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthErrorCode;

/**
 * Raised when a refresh token cannot be exchanged for a new session.
 *
 * <p>Carries the typed {@link AuthErrorCode} explaining why, which the REST layer renders as the
 * machine-readable {@code code} of a 401 response.</p>
 */
public class RefreshTokenException extends RuntimeException {

  private final AuthErrorCode errorCode;

  /**
   * Creates the exception with the default message of its code.
   *
   * @param errorCode the reason the refresh token was rejected; required
   */
  public RefreshTokenException(AuthErrorCode errorCode) {
    super(requireErrorCode(errorCode).getDefaultMessage());
    this.errorCode = errorCode;
  }

  /**
   * Returns the reason the refresh token was rejected.
   *
   * @return the error code, never {@code null}
   */
  public AuthErrorCode getErrorCode() {
    return errorCode;
  }

  private static AuthErrorCode requireErrorCode(AuthErrorCode errorCode) {
    if (errorCode == null) {
      throw new IllegalArgumentException("errorCode must not be null");
    }
    return errorCode;
  }
}
