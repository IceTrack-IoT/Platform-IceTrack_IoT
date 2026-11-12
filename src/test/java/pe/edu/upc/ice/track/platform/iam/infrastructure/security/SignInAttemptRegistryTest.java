package pe.edu.upc.ice.track.platform.iam.infrastructure.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignInAttemptRegistryTest {

  private static final int MAX_FAILURES = 3;
  private static final long WINDOW_MINUTES = 5;

  private SignInAttemptRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new SignInAttemptRegistry(MAX_FAILURES, WINDOW_MINUTES);
  }

  @ParameterizedTest(name = "[{index}] maxFailures={0}")
  @ValueSource(ints = {0, -1})
  @DisplayName("SignInAttemptRegistry(int, long) -> rejects fewer than one allowed failure (AAA)")
  void rejectsInvalidMaxFailures(int maxFailures) {
    var ex = assertThrows(IllegalStateException.class, () -> new SignInAttemptRegistry(maxFailures, 5));
    assertThat(ex).hasMessage("authorization.sign-in.rate-limit.max-failures must be at least 1");
  }

  @ParameterizedTest(name = "[{index}] windowMinutes={0}")
  @ValueSource(longs = {0, -5})
  @DisplayName("SignInAttemptRegistry(int, long) -> rejects a window shorter than one minute (AAA)")
  void rejectsInvalidWindow(long minutes) {
    var ex = assertThrows(IllegalStateException.class, () -> new SignInAttemptRegistry(5, minutes));
    assertThat(ex).hasMessage("authorization.sign-in.rate-limit.window-minutes must be at least 1");
  }

  @Test
  @DisplayName("retryAfter(Collection) -> empty for keys without failures (AAA)")
  void notBlockedWithoutFailures() {
    assertThat(registry.retryAfter(List.of("ip:1.2.3.4"))).isEmpty();
  }

  @Test
  @DisplayName("retryAfter(Collection) -> stays empty below the failure threshold (AAA)")
  void notBlockedBelowThreshold() {
    // Arrange
    registry.recordFailure(List.of("user:jperez"));
    registry.recordFailure(List.of("user:jperez"));

    // Assert
    assertThat(registry.retryAfter(List.of("user:jperez"))).isEmpty();
  }

  @Test
  @DisplayName("retryAfter(Collection) -> blocks once the failure threshold is reached, with a wait inside the window (AAA)")
  void blockedAtThreshold() {
    // Arrange
    for (int i = 0; i < MAX_FAILURES; i++) {
      registry.recordFailure(List.of("user:jperez"));
    }

    // Act
    var retryAfter = registry.retryAfter(List.of("user:jperez"));

    // Assert
    assertThat(retryAfter).isPresent();
    assertThat(retryAfter.get()).isGreaterThan(Duration.ZERO).isLessThanOrEqualTo(Duration.ofMinutes(WINDOW_MINUTES));
  }

  @Test
  @DisplayName("recordFailure(Collection) -> counts every supplied key independently (AAA)")
  void recordsEveryKey() {
    // Arrange
    for (int i = 0; i < MAX_FAILURES; i++) {
      registry.recordFailure(List.of("user:jperez", "ip:1.2.3.4"));
    }

    // Assert
    assertThat(registry.retryAfter(List.of("ip:1.2.3.4"))).isPresent();
    assertThat(registry.retryAfter(List.of("user:jperez"))).isPresent();
    assertThat(registry.retryAfter(List.of("user:other"))).isEmpty();
  }

  @Test
  @DisplayName("retryAfter(Collection) -> blocks when any one of several keys is blocked (AAA)")
  void blockedWhenAnyKeyBlocks() {
    // Arrange
    for (int i = 0; i < MAX_FAILURES; i++) {
      registry.recordFailure(List.of("second"));
    }

    // Act
    var retryAfter = registry.retryAfter(Set.of("first", "second"));

    // Assert
    assertThat(retryAfter).isPresent();
    assertThat(retryAfter.get()).isGreaterThan(Duration.ZERO).isLessThanOrEqualTo(Duration.ofMinutes(WINDOW_MINUTES));
  }

  @Test
  @DisplayName("reset(String) -> clears the failures of a key after a successful sign-in (AAA)")
  void resetClearsKey() {
    // Arrange
    for (int i = 0; i < MAX_FAILURES; i++) {
      registry.recordFailure(List.of("user:jperez"));
    }

    // Act
    registry.reset("user:jperez");

    // Assert
    assertThat(registry.retryAfter(List.of("user:jperez"))).isEmpty();
  }

  @Test
  @DisplayName("reset(String) -> ignores keys that were never tracked (AAA)")
  void resetUnknownKey() {
    registry.reset("never-seen");
    assertThat(registry.retryAfter(List.of("never-seen"))).isEmpty();
  }

  @Test
  @DisplayName("purgeExpiredWindows() -> keeps windows that are still open (AAA)")
  void purgeKeepsOpenWindows() {
    // Arrange
    for (int i = 0; i < MAX_FAILURES; i++) {
      registry.recordFailure(List.of("user:jperez"));
    }

    // Act
    registry.purgeExpiredWindows();

    // Assert
    assertThat(registry.retryAfter(List.of("user:jperez"))).isPresent();
  }

  @Test
  @DisplayName("recordFailure(Collection) -> stops tracking new keys once the registry is full (AAA)")
  void fullRegistryIgnoresNewKeys() {
    // Arrange
    var single = new SignInAttemptRegistry(1, 5);
    for (int i = 0; i < SignInAttemptRegistry.MAX_TRACKED_KEYS; i++) {
      single.recordFailure(List.of("key-" + i));
    }

    // Act
    single.recordFailure(List.of("overflow"));

    // Assert
    assertThat(single.retryAfter(List.of("key-0"))).isPresent();
    assertThat(single.retryAfter(List.of("overflow"))).isEmpty();
  }
}
