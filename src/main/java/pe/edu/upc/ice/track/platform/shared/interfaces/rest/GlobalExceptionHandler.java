package pe.edu.upc.ice.track.platform.shared.interfaces.rest;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.ResolvableType;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.JsonFieldNameAssembler;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
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
   * <p>Each violation is reported under the JSON name of its property ({@code min_celsius}, not the
   * Java field {@code minCelsius}), and the violations are listed once each, in alphabetical order, so
   * the same invalid body always yields the same details.</p>
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
            JsonFieldNameAssembler.toJsonFieldName(error.getField()),
            error.getDefaultMessage()
        ))
        .sorted()
        .distinct()
        .reduce((a, b) -> a + "; " + b)
        .orElse(resolveMessageOrDefault("validation.request.failed", "Request validation failed"));

    var applicationError = ApplicationError.validationError("request-body", errorDetails);
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles a path variable or query parameter whose text cannot be converted to the declared type,
   * such as {@code ?status=ON} for an enum that has no such constant or {@code ?site_id=abc}.
   *
   * <p>Without this handler the conversion failure would fall into the generic
   * {@link RuntimeException} handler and be rendered as a 500, although the request is what is
   * wrong. For an enum parameter the accepted constants are listed, so a client can correct the
   * value without reading the API documentation. That holds for a multi-value parameter too, such
   * as {@code ?status=OPEN,ON}, whose declared type is a collection of the enum.</p>
   *
   * @param ex the type mismatch raised while binding the parameter
   * @return error response with BAD_REQUEST status
   */
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<?> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
    var parameterName = ex.getName();
    var enumType = acceptedEnumType(ex);
    String reason;
    if (enumType != null) {
      var acceptedValues = Arrays.toString(enumType.getEnumConstants());
      reason = resolveMessageOrDefault(
          "validation.request.parameter.enum",
          "Parameter '%s' must be one of %s".formatted(parameterName, acceptedValues),
          parameterName, acceptedValues);
    } else {
      reason = resolveMessageOrDefault(
          "validation.request.parameter.invalid",
          "Parameter '%s' has an invalid value".formatted(parameterName),
          parameterName);
    }
    var applicationError = ApplicationError.validationError(parameterName, reason);
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles a required query parameter that is absent, such as a maintenance window audit sent
   * without {@code equipment_id}.
   *
   * <p>Without this handler the failure would fall into the generic {@link Exception} handler and be
   * rendered as a 500, although the request is what is wrong.</p>
   *
   * @param ex the missing parameter exception raised while binding the request
   * @return error response with BAD_REQUEST status
   */
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<?> handleMissingServletRequestParameter(MissingServletRequestParameterException ex) {
    var parameterName = ex.getParameterName();
    var applicationError = ApplicationError.validationError(
        parameterName,
        resolveMessageOrDefault(
            "validation.request.parameter.missing",
            "Parameter '%s' is required".formatted(parameterName),
            parameterName));
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError);
  }

  /**
   * Handles a request body that cannot be read, either because it is not well-formed JSON or
   * because a value does not fit its field, such as an enum constant the API does not define.
   *
   * <p>The exception message is not echoed because it names internal classes.</p>
   *
   * @param ex the read failure raised while converting the body
   * @return error response with BAD_REQUEST status
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<?> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
    var applicationError = ApplicationError.validationError(
        "request-body",
        resolveMessageOrDefault(
            "validation.request.body.unreadable",
            "The request body is malformed or holds a value outside the accepted set"));
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
    var applicationError = ApplicationError.unexpected(
        resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
        "Data integrity violation not caused by a unique constraint");
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError, ex);
  }

  /**
   * Handles unexpected runtime exceptions not caught by specific handlers.
   *
   * <p>The exception is logged with its stack trace under a reference; the client receives a generic
   * {@code UNEXPECTED_ERROR} carrying only that reference, never the exception message.</p>
   *
   * @param ex the unhandled runtime exception
   * @return error response with INTERNAL_SERVER_ERROR status
   */
  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<?> handleRuntimeException(RuntimeException ex) {
    return toUnexpectedErrorResponse(ex);
  }

  /**
   * Handles all other exceptions not matched by specific handlers.
   * Provides a final fallback for any unexpected exception type, with the same sanitizing as
   * {@link #handleRuntimeException(RuntimeException)}.
   *
   * @param ex the exception
   * @return error response with INTERNAL_SERVER_ERROR status
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<?> handleException(Exception ex) {
    return toUnexpectedErrorResponse(ex);
  }

  private ResponseEntity<?> toUnexpectedErrorResponse(Exception ex) {
    var applicationError = ApplicationError.unexpected(
        resolveMessageOrDefault("error.unexpected.context", "global-exception-handler"),
        "Unhandled " + ex.getClass().getName());
    return ErrorResponseAssembler.toErrorResponseFromApplicationError(applicationError, ex);
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

  /**
   * Finds the enum a mismatched parameter accepts: its declared type, or the element type of the
   * collection it is declared as.
   *
   * @param ex the type mismatch raised while binding the parameter
   * @return the enum type, or {@code null} when the parameter is not an enum nor a collection of one
   */
  private static @Nullable Class<?> acceptedEnumType(MethodArgumentTypeMismatchException ex) {
    var requiredType = ex.getRequiredType();
    if (requiredType == null) {
      return null;
    }
    if (requiredType.isEnum()) {
      return requiredType;
    }
    if (Collection.class.isAssignableFrom(requiredType)) {
      var elementType = ResolvableType.forMethodParameter(ex.getParameter()).asCollection().resolveGeneric(0);
      return elementType != null && elementType.isEnum() ? elementType : null;
    }
    return null;
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
