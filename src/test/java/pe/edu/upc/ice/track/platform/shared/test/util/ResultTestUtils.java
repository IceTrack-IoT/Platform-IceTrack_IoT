package pe.edu.upc.ice.track.platform.shared.test.util;

import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Centralized helper to unwrap {@link Result} values in unit tests.
 *
 * <p>Application services in this code base report rejected writes as a
 * {@link Result.Failure} value instead of throwing, so almost every command-service test needs to
 * read either the success payload or the {@code ApplicationError}. These helpers fail LOUDLY, with
 * the offending {@link Result} in the message, when the test received the opposite variant.
 *
 * <p>This class is intentionally final and has a private constructor: it is a stateless utility.
 */
public final class ResultTestUtils {

  private ResultTestUtils() {
    throw new AssertionError("ResultTestUtils is a utility class and must not be instantiated.");
  }

  /**
   * Returns the payload of a successful result.
   *
   * @throws AssertionError if the result is a {@link Result.Failure}
   */
  public static <T, E> T successValue(Result<T, E> result) {
    if (result instanceof Result.Success<T, E> success) {
      return success.value();
    }
    throw new AssertionError("Expected a Result.Success but was: " + result);
  }

  /**
   * Returns the error of a failed result.
   *
   * @throws AssertionError if the result is a {@link Result.Success}
   */
  public static <T, E> E failureError(Result<T, E> result) {
    if (result instanceof Result.Failure<T, E> failure) {
      return failure.error();
    }
    throw new AssertionError("Expected a Result.Failure but was: " + result);
  }
}
