package pe.edu.upc.ice.track.platform.shared.interfaces.rest;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Global exception handler for REST API.
 * Provides centralized exception handling for the entire application,
 * ensuring all unhandled exceptions are translated to consistent
 * HTTP responses via the shared error assembly pattern.
 */
@RestControllerAdvice
@NullMarked
@Slf4j
public class GlobalExceptionHandler {
  private static final String MESSAGES_BASENAME = "messages";
  // PostgreSQL's SQLSTATE for unique_violation
  private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

  /**
   * Handles validation exceptions from Spring's request body validation.
   * Maps validation failure to a standardized error response.
   *
   * @param ex the validation exception from @Valid binding
   * @return error response with BAD_REQUEST status
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
    var fieldErrors = ex.getBindingResult().getFieldErrors();
    var validationPrefix = resolveMessageOrDefault("validation.field.prefix", "Field");
    var errorDetails = fieldErrors.isEmpty()
        ? resolveMessageOrDefault("validation.request.failed", "Request validation failed")
        : fieldErrors.stream()
        .map(error -> "%s %s: %s".formatted(
            validationPrefix,
            error.getField(),
            error.getDefaultMessage()
        ))
        .reduce((a, b) -> a + "; " + b)
        .orElse(resolveMessageOrDefault("validation.request.failed", "Request validation failed"));

    var applicationError = ApplicationError.validationError("request-body", errorDetails);
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles invalid request arguments such as malformed UUID path or payload values.
   *
   * @param ex the illegal argument exception
   * @return error response with BAD_REQUEST status
   */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> handleIllegalArgumentException(IllegalArgumentException ex) {
    var applicationError = ApplicationError.validationError(
        resolveMessageOrDefault("validation.request.argument", "request-argument"),
        ex.getMessage() != null ? ex.getMessage() : resolveMessageOrDefault("validation.request.failed", "Request validation failed")
    );
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles authorization denials raised by method security ({@code @PreAuthorize}).
   *
   * <p>Such denials are thrown while the controller method is invoked, so they reach this advice
   * before Spring Security's exception translation; without this handler they would fall into the
   * generic {@link RuntimeException} handler and be rendered as a 500.</p>
   *
   * @param ex the access denied exception
   * @return error response with FORBIDDEN status
   */
  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<?> handleAccessDeniedException(AccessDeniedException ex) {
    var applicationError = ApplicationError.accessDenied(
        resolveMessageOrDefault("error.access-denied.reason", "You are not allowed to perform this operation"));
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles optimistic locking failures, including the JPA-specific
   * {@code ObjectOptimisticLockingFailureException} subclass.
   *
   * <p>Raised when a {@code @Version}-checked write finds the row changed after it was read - for
   * instance an owner editing a site at the instant its background geocoding is recorded. Nothing
   * failed on the server: the caller acted on a stale copy and only has to re-read and retry, so
   * 409 Conflict is returned. The exception message is not echoed because it names the
   * persistence entity and its identifier.</p>
   *
   * @param ex the optimistic locking failure
   * @return error response with CONFLICT status
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ResponseEntity<?> handleOptimisticLockingFailure(OptimisticLockingFailureException ex) {
    var applicationError = ApplicationError.concurrentModification(
        resolveMessageOrDefault("error.concurrent-modification.reason", "Reload the resource and retry the operation"));
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles integrity violations rejected by the database, including the
   * {@link DuplicateKeyException} subclass.
   *
   * <p>A unique violation is the database settling a race that an application-level existence
   * check cannot see - two requests registering the same equipment uid at the same instant both
   * pass the check, and the unique index rejects the second insert. The resource the caller wanted
   * to create already exists, so 409 Conflict is returned. Any other violation (not-null, check or
   * foreign key) means a domain rule was bypassed, so it is still answered as an unexpected error
   * and logged for diagnosis.</p>
   *
   * <p>The exception message is never echoed, in either case, because it carries the SQL statement
   * and the table and constraint names.</p>
   *
   * @param ex the data integrity violation
   * @return error response with CONFLICT status for a unique violation, INTERNAL_SERVER_ERROR otherwise
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<?> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
    if (isUniqueViolation(ex)) {
      var applicationError = ApplicationError.duplicateResource(
          resolveMessageOrDefault("error.duplicate-resource.reason", "Verify unique attributes such as the equipment UID and try again"));
      return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
    }
    log.error("Data integrity violation not caused by a unique constraint", ex);
    var applicationError = ApplicationError.unexpected(
        resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
        resolveMessageOrDefault("error.data-integrity.reason", "The data could not be stored"));
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles unexpected runtime exceptions not caught by specific handlers.
   * Maps to a generic unexpected error response.
   *
   * @param ex the unhandled runtime exception
   * @return error response with INTERNAL_SERVER_ERROR status
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
    var applicationError = ApplicationError.unexpected(
        resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
        ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
    );
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles all other exceptions not matched by specific handlers.
   * Provides a final fallback for any unexpected exception type.
   *
   * @param ex the exception
   * @return error response with INTERNAL_SERVER_ERROR status
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception ex) {
    var applicationError = ApplicationError.unexpected(
        resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
        ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred"
    );
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Tells whether an integrity violation was raised by a unique constraint or index.
   *
   * <p>Decided on the SQLSTATE reported by the driver rather than on the message, which the
   * database server localizes and rewords between versions.</p>
   *
   * @param ex the data integrity violation
   * @return {@code true} when a uniqueness rule rejected the write
   */
  private static boolean isUniqueViolation(DataIntegrityViolationException ex) {
    return ex instanceof DuplicateKeyException
        || (ex.getMostSpecificCause() instanceof SQLException sqlException
            && UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState()));
  }

  private String resolveMessageOrDefault(String key, String defaultValue, Object... args) {
    try {
      var bundle = ResourceBundle.getBundle(MESSAGES_BASENAME, LocaleContextHolder.getLocale());
      if (!bundle.containsKey(key)) {
        return defaultValue;
      }
      return MessageFormat.format(bundle.getString(key), args);
    } catch (MissingResourceException ex) {
      return defaultValue;
    }
  }
}
