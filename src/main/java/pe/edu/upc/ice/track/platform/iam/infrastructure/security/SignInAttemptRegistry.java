package pe.edu.upc.ice.track.platform.iam.infrastructure.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory registry of failed sign-in attempts, keyed by client address and by account
 * identifier.
 *
 * <p>Each key owns a fixed window that opens with its first failure and lasts
 * {@code authorization.sign-in.rate-limit.window-minutes} (5 by default). Once a key collects
 * {@code authorization.sign-in.rate-limit.max-failures} failures (5 by default) inside its window,
 * it is blocked until the window ends. A window that has ended is discarded, so the next failure
 * opens a new one.</p>
 *
 * <p>State lives in the memory of this instance: with several instances behind a load balancer,
 * each one enforces the limit on the requests it receives. Expired windows are purged every
 * minute, and the number of tracked keys is capped, so a flood of distinct addresses or usernames
 * cannot exhaust the heap.</p>
 */
@Component
@Slf4j
public class SignInAttemptRegistry {

  /** Upper bound on tracked keys; beyond it, new keys are not tracked until expired ones are purged. */
  static final int MAX_TRACKED_KEYS = 100_000;

  private final int maxFailures;
  private final Duration window;
  private final ConcurrentMap<String, FailureWindow> windows = new ConcurrentHashMap<>();

  /**
   * Creates the registry.
   *
   * @param maxFailures   failures a key may collect inside one window before it is blocked
   * @param windowMinutes length of a window, in minutes
   * @throws IllegalStateException when either value is not positive
   */
  public SignInAttemptRegistry(
      @Value("${authorization.sign-in.rate-limit.max-failures:5}") int maxFailures,
      @Value("${authorization.sign-in.rate-limit.window-minutes:5}") long windowMinutes) {
    if (maxFailures < 1) {
      throw new IllegalStateException("authorization.sign-in.rate-limit.max-failures must be at least 1");
    }
    if (windowMinutes < 1) {
      throw new IllegalStateException("authorization.sign-in.rate-limit.window-minutes must be at least 1");
    }
    this.maxFailures = maxFailures;
    this.window = Duration.ofMinutes(windowMinutes);
  }

  /**
   * Tells how long the caller must wait before it may try again.
   *
   * @param keys the keys the request is attributed to
   * @return the longest remaining block among the keys, or empty when none of them is blocked
   */
  public Optional<Duration> retryAfter(Collection<String> keys) {
    var now = Instant.now();
    return keys.stream()
        .map(windows::get)
        .filter(Objects::nonNull)
        .filter(failureWindow -> failureWindow.blocks(now, window, maxFailures))
        .map(failureWindow -> failureWindow.remaining(now, window))
        .max(Comparator.naturalOrder());
  }

  /**
   * Counts one failed attempt against every key.
   *
   * @param keys the keys the failed request is attributed to
   */
  public void recordFailure(Collection<String> keys) {
    var now = Instant.now();
    for (var key : keys) {
      if (!windows.containsKey(key) && windows.size() >= MAX_TRACKED_KEYS) {
        purgeExpiredWindows();
        if (windows.size() >= MAX_TRACKED_KEYS) {
          log.warn("Sign-in attempt registry is full ({} keys); not tracking a new key", windows.size());
          continue;
        }
      }
      windows.compute(key, (ignored, current) -> current == null || current.hasEnded(now, window)
          ? new FailureWindow(now, 1)
          : current.withOneMoreFailure());
    }
  }

  /**
   * Forgets the failures of a key, typically after a successful sign-in of that account.
   *
   * @param key the key to clear
   */
  public void reset(String key) {
    windows.remove(key);
  }

  /**
   * Discards every window that has ended.
   */
  @Scheduled(fixedDelay = 60_000)
  public void purgeExpiredWindows() {
    var now = Instant.now();
    windows.values().removeIf(failureWindow -> failureWindow.hasEnded(now, window));
  }

  /**
   * Failures counted for one key since its window opened.
   *
   * @param openedAt the instant of the first failure of the window
   * @param failures the number of failures counted in the window
   */
  private record FailureWindow(Instant openedAt, int failures) {

    boolean hasEnded(Instant now, Duration window) {
      return !now.isBefore(openedAt.plus(window));
    }

    boolean blocks(Instant now, Duration window, int maxFailures) {
      return !hasEnded(now, window) && failures >= maxFailures;
    }

    Duration remaining(Instant now, Duration window) {
      return Duration.between(now, openedAt.plus(window));
    }

    FailureWindow withOneMoreFailure() {
      return failures == Integer.MAX_VALUE ? this : new FailureWindow(openedAt, failures + 1);
    }
  }
}
