package pe.edu.upc.ice.track.platform.shared.test.fixtures;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Centralized, context-agnostic fixtures: generic valid scalars and {@link Stream} of
 * {@link Arguments} suppliers for {@code @ParameterizedTest @MethodSource} declarations.
 *
 * <p>Per-context fixture classes (e.g. {@code AssetsCommandFixtures}) MUST consume the constants
 * and method sources defined here and MUST NOT redefine the same boundary values locally.
 */
public final class CommonCommandFixtures {

  private CommonCommandFixtures() {
    throw new AssertionError("CommonCommandFixtures is a utility class and must not be instantiated.");
  }

  // ---------- Valid scalar fixtures ----------

  public static final Long VALID_ID = 1L;
  public static final Long ANOTHER_VALID_ID = 2L;

  // ---------- Parameterized sources: invalid strings ----------

  /** Strings that must be rejected as "blank": {@code null}, empty and whitespace-only values. */
  public static Stream<Arguments> invalidBlankStrings() {
    return Stream.of(
        Arguments.of((Object) null),
        Arguments.of(""),
        Arguments.of(" "),
        Arguments.of("   "),
        Arguments.of("\t"),
        Arguments.of("\n")
    );
  }

  // ---------- Parameterized sources: numeric boundaries ----------

  /** Identifier boundary values: zero, negative and {@link Long#MIN_VALUE}. */
  public static Stream<Arguments> nonPositiveIds() {
    return Stream.of(
        Arguments.of(0L),
        Arguments.of(-1L),
        Arguments.of(Long.MIN_VALUE)
    );
  }
}
