package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.TelemetryBucketSize;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.time.Instant;
import java.util.Objects;

/**
 * Query to read the telemetry history of a unit aggregated into time buckets.
 *
 * <p>A fixed width that would cut the range into more than {@value TelemetryBucketSize#MAX_BUCKETS}
 * buckets is refused. Without a width ({@code auto}) the smallest width keeping the answer at or
 * below {@code maxPoints} is chosen.</p>
 *
 * @param equipmentId identifier of the unit; required
 * @param from        inclusive start of the range; required
 * @param to          inclusive end of the range; required and not before {@code from}
 * @param bucketSize  the width of the buckets, or {@code null} for {@code auto}
 * @param maxPoints   the most buckets {@code auto} may answer, from {@value #MIN_POINTS} to
 *                    {@value TelemetryBucketSize#MAX_BUCKETS}; {@code null} means
 *                    {@value #DEFAULT_MAX_POINTS}
 * @param username    username of the authenticated caller
 * @param callerRole  the role the caller acts in
 */
public record GetReadingBucketsByEquipmentQuery(
    Long equipmentId,
    Instant from,
    Instant to,
    TelemetryBucketSize bucketSize,
    Integer maxPoints,
    String username,
    CallerRole callerRole) {

  /** Fewest points {@code max_points} accepts. */
  public static final int MIN_POINTS = 10;

  /** Points {@code auto} aims at when the client sets none. */
  public static final int DEFAULT_MAX_POINTS = 500;

  /**
   * Validates the range, the point budget and the number of buckets a fixed width yields.
   *
   * @throws IllegalArgumentException when a value is out of range
   */
  public GetReadingBucketsByEquipmentQuery {
    Objects.requireNonNull(from, "from must not be null");
    Objects.requireNonNull(to, "to must not be null");
    if (from.isAfter(to)) {
      throw new IllegalArgumentException("Parameter 'from' must not be after 'to'");
    }
    maxPoints = maxPoints == null ? DEFAULT_MAX_POINTS : maxPoints;
    if (maxPoints < MIN_POINTS || maxPoints > TelemetryBucketSize.MAX_BUCKETS) {
      throw new IllegalArgumentException("Parameter 'max_points' must be between %d and %d"
          .formatted(MIN_POINTS, TelemetryBucketSize.MAX_BUCKETS));
    }
    if (bucketSize != null && bucketSize.bucketCount(from, to) > TelemetryBucketSize.MAX_BUCKETS) {
      throw new IllegalArgumentException("Parameter 'bucket' %s would cut the range into more than %d buckets; use a wider bucket or auto"
          .formatted(bucketSize.label(), TelemetryBucketSize.MAX_BUCKETS));
    }
  }

  /**
   * @return the width to aggregate with: the one asked for, or the smallest that fits the budget
   */
  public TelemetryBucketSize effectiveBucketSize() {
    return bucketSize != null ? bucketSize : TelemetryBucketSize.smallestFitting(from, to, maxPoints);
  }
}
