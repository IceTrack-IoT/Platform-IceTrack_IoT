package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import java.util.List;
import java.util.Objects;

/**
 * A unit's telemetry history aggregated into buckets, with the width the buckets have.
 *
 * @param bucketSize the width of the buckets, the one {@code auto} chose when it was asked for
 * @param buckets    the non-empty buckets, oldest first
 */
public record TelemetryBucketSeries(TelemetryBucketSize bucketSize, List<TelemetryBucket> buckets) {

  public TelemetryBucketSeries {
    Objects.requireNonNull(bucketSize, "bucketSize must not be null");
    buckets = buckets == null ? List.of() : List.copyOf(buckets);
  }
}
