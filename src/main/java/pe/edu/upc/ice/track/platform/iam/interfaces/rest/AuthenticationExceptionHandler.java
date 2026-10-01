package pe.edu.upc.ice.track.platform.iam.interfaces.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.resources.AuthErrorResource;
import pe.edu.upc.ice.track.platform.iam.interfaces.rest.transform.AuthErrorResourceFromExceptionAssembler;

/**
 * Exception handler of the IAM authentication endpoints.
 *
 * <p>Renders every rejected refresh token as a 401 {@link AuthErrorResource} whose {@code code} is
 * the exact name of the
 * {@link pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthErrorCode}, so HTTP
 * interceptors can react to it without parsing messages.</p>
 *
 * <p>Ordered first: Spring uses the first advice that has <em>any</em> matching handler, and the
 * shared {@code GlobalExceptionHandler} has a catch-all {@code RuntimeException} handler that would
 * otherwise turn these rejections into a 500. Scoped to {@link AuthenticationController}, so the
 * error contract of every other endpoint is left unchanged.</p>
 */
@RestControllerAdvice(assignableTypes = AuthenticationController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class AuthenticationExceptionHandler {

  /**
   * Handles a rejected refresh token.
   *
   * @param exception the rejection, carrying its typed error code
   * @return a 401 response carrying the machine-readable code
   */
  @ExceptionHandler(RefreshTokenException.class)
  public ResponseEntity<AuthErrorResource> handleRefreshTokenException(RefreshTokenException exception) {
    log.info("Refresh token rejected: {}", exception.getErrorCode());
    var status = HttpStatus.UNAUTHORIZED;
    return ResponseEntity
        .status(status)
        .body(AuthErrorResourceFromExceptionAssembler.toResourceFromException(exception, status));
  }
}
