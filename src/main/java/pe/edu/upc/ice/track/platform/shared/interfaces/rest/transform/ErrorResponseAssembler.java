package pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.resources.ErrorResource;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.UUID;

/**
 * Assembler for converting application errors to HTTP responses.
 *
 * <p>Server errors (any 5xx) never reach the client with their details: whatever an application
 * service or exception handler put there (exception messages, SQL fragments, class names) is
 * logged together with a random reference, and the client receives only that reference, which
 * support can use to find the log entry.</p>
 */
@NullMarked
@Slf4j
public final class ErrorResponseAssembler {
  private static final String MESSAGES_BASENAME = "messages";

  private ErrorResponseAssembler() {
  }

  /**
   * Maps an ApplicationError to an appropriate HTTP ResponseEntity.
   * Automatically selects the correct HTTP status code based on the error code.
   *
   * @param error the ApplicationError to map
   * @return a ResponseEntity with the appropriate HTTP status and error resource
   */
  public static ResponseEntity<ErrorResource> toErrorResponseFromApplicationError(ApplicationError error) {
    return toErrorResponseFromApplicationError(error, null);
  }

  /**
   * Maps an ApplicationError to an HTTP response, logging the cause of a server error.
   *
   * <p>For a 5xx status the error, its details and the cause (when given) are logged under a new
   * reference, and the response carries the reference instead of the details.</p>
   *
   * @param error the ApplicationError to map
   * @param cause the exception behind the error, logged with its stack trace; may be {@code null}
   * @return a ResponseEntity with the appropriate HTTP status and error resource
   */
  public static ResponseEntity<ErrorResource> toErrorResponseFromApplicationError(
      ApplicationError error, @Nullable Throwable cause) {
    HttpStatusCode status = toStatusFromErrorCode(error.code());
    ApplicationError exposed = status.is5xxServerError() ? withReferenceInsteadOfDetails(error, cause) : error;
    String localizedMessage = toLocalizedMessageFromApplicationError(exposed);
    ErrorResource resource = new ErrorResource(exposed.code(), localizedMessage, exposed.details());
    return new ResponseEntity<>(resource, status);
  }

  private static ApplicationError withReferenceInsteadOfDetails(ApplicationError error, @Nullable Throwable cause) {
    var reference = UUID.randomUUID().toString();
    if (cause != null) {
      log.error("Server error {} [reference {}]: {} - {}", error.code(), reference, error.message(), error.details(), cause);
    } else {
      log.error("Server error {} [reference {}]: {} - {}", error.code(), reference, error.message(), error.details());
    }
    return new ApplicationError(error.code(), error.message(), "Reference: " + reference);
  }

  private static String toLocalizedMessageFromApplicationError(ApplicationError error) {
    String specificKey = toSpecificMessageKeyFromErrorCode(error.code());
    return toLocalizedMessageOrNull(specificKey, error.details(), toEntityNameFromErrorCode(error.code()));

  }

  private static String toSpecificMessageKeyFromErrorCode(String errorCode) {
    return "error.%s.message".formatted(errorCode.toLowerCase(Locale.ROOT).replace('_', '-'));
  }

  private static String toMessageKeyFromErrorCode(String errorCode) {
    return switch (errorCode) {
      case "VALIDATION_ERROR" -> "error.validation.message";
      case "BUSINESS_RULE_VIOLATION" -> "error.business-rule.message";
      case "UNEXPECTED_ERROR" -> "error.unexpected.message";
      case String s when s.endsWith("_NOT_FOUND") -> "error.not-found.message";
      case String s when s.endsWith("_CONFLICT") -> "error.conflict.message";
      default -> "error.generic.message";
    };
  }

  private static String toEntityNameFromErrorCode(String errorCode) {
    if (errorCode.endsWith("_NOT_FOUND")) {
      return errorCode.replace("_NOT_FOUND", "").toLowerCase(Locale.ROOT);
    }
    if (errorCode.endsWith("_CONFLICT")) {
      return errorCode.replace("_CONFLICT", "").toLowerCase(Locale.ROOT);
    }
    return "resource";
  }

  private static @Nullable String toLocalizedMessageOrNull(String key, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BASENAME, locale);
      if (!bundle.containsKey(key)) {
        return null;
      }
      String template = bundle.getString(key);
      return MessageFormat.format(template, args);
    } catch (MissingResourceException ex) {
      return null;
    }
  }

  private static String toLocalizedMessageWithFallback(String key, String fallback, Object... args) {
    Locale locale = LocaleContextHolder.getLocale();
    try {
      ResourceBundle bundle = ResourceBundle.getBundle(MESSAGES_BASENAME, locale);
      if (!bundle.containsKey(key)) {
        return fallback;
      }
      String template = bundle.getString(key);
      return MessageFormat.format(template, args);
    } catch (MissingResourceException ex) {
      return fallback;
    }
  }

  /**
   * Determines the appropriate HTTP status code for a given error code.
   *
   * <p>Conflicts are recognized by their {@code _CONFLICT} suffix, except for the state machine
   * codes of the {@code service.request} context, whose names describe the rule that was broken
   * rather than the resource, and which are listed explicitly.</p>
   *
   * @param errorCode the error code string (e.g., "PROFILE_NOT_FOUND", "VALIDATION_ERROR")
   * @return the corresponding HttpStatus
   */
  public static HttpStatusCode toStatusFromErrorCode(String errorCode) {
    return switch (errorCode) {
      case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
      case "INVALID_STATUS_TRANSITION", "TECHNICIAN_NOT_ELIGIBLE", "REVIEW_NOT_ALLOWED" -> HttpStatus.CONFLICT;
      case String s when s.endsWith("_NOT_FOUND") -> HttpStatus.NOT_FOUND;
      case "BUSINESS_RULE_VIOLATION" -> HttpStatusCode.valueOf(422);
      case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
      case "ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
      case String s when s.endsWith("_CONFLICT") -> HttpStatus.CONFLICT;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
