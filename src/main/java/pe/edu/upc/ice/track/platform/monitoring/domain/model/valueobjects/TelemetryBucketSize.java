package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Width of the time buckets a telemetry history is aggregated into.
 *
 * <p>Buckets are aligned to the epoch in UTC: a bucket of width {@code w} starts at a multiple of
 * {@code w}, so the same reading always falls in the same bucket whatever range is asked for.</p>
 */
public enum TelemetryBucketSize {
  ONE_MINUTE("1m", Duration.ofMinutes(1)),
  FIVE_MINUTES("5m", Duration.ofMinutes(5)),
  FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
  ONE_HOUR("1h", Duration.ofHours(1)),
  SIX_HOURS("6h", Duration.ofHours(6));

  /** Most buckets one answer may hold. */
  public static final int MAX_BUCKETS = 2_000;

  private final String label;
  private final Duration width;

  TelemetryBucketSize(String label, Duration width) {
    this.label = label;
    this.width = width;
  }

  /** @return the label the API uses for this width, e.g. {@code 1h} */
  public String label() {
    return label;
  }

  /** @return the width of one bucket */
  public Duration width() {
    return width;
  }

  /**
   * Finds the width a label names.
   *
   * @param label the label, e.g. {@code 15m}
   * @return the width, or empty when no width carries that label
   */
  public static Optional<TelemetryBucketSize> fromLabel(String label) {
    return Arrays.stream(values()).filter(size -> size.label.equals(label)).findFirst();
  }

  /**
   * Counts the aligned buckets a range touches, empty or not: the most points an answer over the
   * range can hold.
   *
   * @param from inclusive start of the range; required
   * @param to   inclusive end of the range; required and not before {@code from}
   * @return the number of buckets
   */
  public long bucketCount(Instant from, Instant to) {
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    var seconds = width.toSeconds();
    return Math.floorDiv(to.getEpochSecond(), seconds) - Math.floorDiv(from.getEpochSecond(), seconds) + 1;
  }

  /**
   * Picks the smallest width that keeps an answer over a range at or below a number of points; the
   * widest one when none does.
   *
   * @param from      inclusive start of the range; required
   * @param to        inclusive end of the range; required
   * @param maxPoints the most points the answer may hold
   * @return the width
   */
  public static TelemetryBucketSize smallestFitting(Instant from, Instant to, int maxPoints) {
    return Arrays.stream(values())
        .filter(size -> size.bucketCount(from, to) <= maxPoints)
        .findFirst()
        .orElse(SIX_HOURS);
  }

  /**
   * @param instant an instant
   * @return the start of the aligned bucket the instant falls in
   */
  public Instant bucketStartOf(Instant instant) {
    var seconds = width.toSeconds();
    return Instant.ofEpochSecond(Math.floorDiv(instant.getEpochSecond(), seconds) * seconds);
  }
}
