package pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform;

import org.springframework.http.HttpStatus;
import pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.AuthErrorResource;

import java.time.Instant;

/**
 * Assembler that translates a {@link RefreshTokenException} into an {@link AuthErrorResource}.
 */
public class AuthErrorResourceFromExceptionAssembler {

  /**
   * Creates the error resource of a rejected refresh token.
   *
   * @param exception the rejection, carrying its typed error code
   * @param status    the HTTP status the rejection is answered with
   * @return the error resource, whose {@code code} is the exact name of the error code
   */
  public static AuthErrorResource toResourceFromException(RefreshTokenException exception, HttpStatus status) {
    var errorCode = exception.getErrorCode();
    return new AuthErrorResource(
        Instant.now().toString(),
        status.value(),
        status.getReasonPhrase(),
        errorCode.name(),
        errorCode.getDefaultMessage());
  }
}
