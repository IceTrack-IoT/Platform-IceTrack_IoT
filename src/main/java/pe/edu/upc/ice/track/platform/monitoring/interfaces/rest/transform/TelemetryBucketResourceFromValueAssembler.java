package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.TelemetryBucket;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.TelemetryBucketResource;

/**
 * Converts a {@link TelemetryBucket} into a {@link TelemetryBucketResource}.
 */
public final class TelemetryBucketResourceFromValueAssembler {

  private TelemetryBucketResourceFromValueAssembler() {
  }

  /**
   * @param bucket the aggregated bucket
   * @return the equivalent resource
   */
  public static TelemetryBucketResource toResourceFromValue(TelemetryBucket bucket) {
    return new TelemetryBucketResource(
        bucket.bucketStart(),
        bucket.bucketEnd(),
        bucket.minTemperature(),
        bucket.maxTemperature(),
        bucket.avgTemperature(),
        bucket.avgHumidity(),
        bucket.sampleCount(),
        bucket.batchCount(),
        bucket.underMaintenance(),
        bucket.doorOpenSeconds(),
        bucket.lastReceivedAt());
  }
}
