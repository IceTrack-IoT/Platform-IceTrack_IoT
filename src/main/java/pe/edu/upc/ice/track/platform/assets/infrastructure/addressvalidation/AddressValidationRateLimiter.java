package pe.edu.upc.ice.track.platform.assets.infrastructure.addressvalidation;

import org.springframework.stereotype.Component;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.addressvalidation.ValidationRateLimiterPort;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window limiter for address validations: at most {@value #MAX_ATTEMPTS} attempts
 * per key within any {@value #WINDOW_SECONDS}-second window.
 *
 * <p>Counters live in this instance only, as the sign-in throttling does. Each key keeps the instants
 * of its recent attempts; those older than the window are dropped on every call, so memory is bounded
 * by the attempts of the last minute.</p>
 */
@Component
public class AddressValidationRateLimiter implements ValidationRateLimiterPort {

  /** Attempts allowed per key within one window. */
  static final int MAX_ATTEMPTS = 20;

  /** Length of the sliding window, in seconds. */
  static final long WINDOW_SECONDS = 60;

  private static final Duration WINDOW = Duration.ofSeconds(WINDOW_SECONDS);

  private final Clock clock;
  private final Map<String, Deque<Instant>> attempts = new ConcurrentHashMap<>();

  public AddressValidationRateLimiter() {
    this(Clock.systemUTC());
  }

  AddressValidationRateLimiter(Clock clock) {
    this.clock = clock;
  }

  // inherited javadoc
  @Override
  public boolean tryAcquire(String key) {
    var now = clock.instant();
    var window = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
    synchronized (window) {
      while (!window.isEmpty() && window.peekFirst().plus(WINDOW).isBefore(now)) {
        window.pollFirst();
      }
      if (window.size() >= MAX_ATTEMPTS) {
        return false;
      }
      window.addLast(now);
      return true;
    }
  }
}
