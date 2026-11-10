package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/**
 * The reading batches of one unit that fall in one time bucket, aggregated.
 *
 * <p>A batch falls in the bucket of the instant it was taken ({@code recorded_at}). Averages are
 * weighted by each batch's {@code sample_count}, so a batch of 60 samples weighs as much as six
 * batches of 10; a batch that reports no sample count weighs as one sample.</p>
 *
 * @param bucketStart      inclusive start of the bucket
 * @param bucketEnd        exclusive end of the bucket
 * @param minTemperature   lowest of the batches' minimum temperatures, in Celsius
 * @param maxTemperature   highest of the batches' maximum temperatures, in Celsius
 * @param avgTemperature   average of the batches' average temperatures, weighted by sample count
 * @param avgHumidity      weighted average humidity, or {@code null} when no batch reported one
 * @param sampleCount      sum of the batches' samples
 * @param batchCount       number of batches aggregated
 * @param underMaintenance whether any aggregated batch was taken under maintenance
 * @param doorOpenSeconds  sum of the seconds the door was open, or {@code null} when no batch reported it
 * @param lastReceivedAt   latest server arrival time among the batches
 */
public record TelemetryBucket(
    Instant bucketStart,
    Instant bucketEnd,
    double minTemperature,
    double maxTemperature,
    double avgTemperature,
    Double avgHumidity,
    long sampleCount,
    int batchCount,
    boolean underMaintenance,
    Long doorOpenSeconds,
    Instant lastReceivedAt) {

  /**
   * Aggregates reading batches into buckets of a width. Empty buckets are left out, so a gap in the
   * answer is a real gap in reporting. A batch without an average temperature carries nothing to
   * aggregate and is skipped.
   *
   * @param readings the batches, in any order
   * @param size     the width of the buckets; required
   * @return the non-empty buckets, oldest first
   */
  public static List<TelemetryBucket> aggregate(List<SensorReading> readings, TelemetryBucketSize size) {
    Objects.requireNonNull(size, "size must not be null");
    if (readings == null || readings.isEmpty()) {
      return List.of();
    }
    var groups = new TreeMap<Instant, List<SensorReading>>();
    for (var reading : readings) {
      if (reading == null || reading.getRecordedAt() == null || celsiusOf(reading.getAvgTemperature()) == null) {
        continue;
      }
      groups.computeIfAbsent(size.bucketStartOf(reading.getRecordedAt()), start -> new ArrayList<>()).add(reading);
    }
    var buckets = new ArrayList<TelemetryBucket>(groups.size());
    groups.forEach((start, batches) -> buckets.add(of(start, start.plus(size.width()), batches)));
    return List.copyOf(buckets);
  }

  private static TelemetryBucket of(Instant start, Instant end, List<SensorReading> batches) {
    var min = Double.POSITIVE_INFINITY;
    var max = Double.NEGATIVE_INFINITY;
    var weightedTemperature = 0d;
    var temperatureWeight = 0L;
    var weightedHumidity = 0d;
    var humidityWeight = 0L;
    var samples = 0L;
    var underMaintenance = false;
    Long doorOpenSeconds = null;
    Instant lastReceivedAt = null;
    for (var batch : batches) {
      var avg = celsiusOf(batch.getAvgTemperature());
      var batchMin = celsiusOf(batch.getMinTemperature());
      var batchMax = celsiusOf(batch.getMaxTemperature());
      min = Math.min(min, batchMin == null ? avg : batchMin);
      max = Math.max(max, batchMax == null ? avg : batchMax);
      var weight = batch.getSampleCount() == null || batch.getSampleCount() < 1 ? 1L : batch.getSampleCount().longValue();
      weightedTemperature += avg * weight;
      temperatureWeight += weight;
      samples += batch.getSampleCount() == null ? 0L : batch.getSampleCount();
      var humidity = batch.getHumidity() == null ? null : batch.getHumidity().percentage();
      if (humidity != null) {
        weightedHumidity += humidity * weight;
        humidityWeight += weight;
      }
      underMaintenance |= batch.isUnderMaintenance();
      var door = batch.getEnvironment() == null ? null : batch.getEnvironment().doorOpenSeconds();
      if (door != null) {
        doorOpenSeconds = (doorOpenSeconds == null ? 0L : doorOpenSeconds) + door;
      }
      if (batch.getReceivedAt() != null && (lastReceivedAt == null || batch.getReceivedAt().isAfter(lastReceivedAt))) {
        lastReceivedAt = batch.getReceivedAt();
      }
    }
    return new TelemetryBucket(
        start,
        end,
        min,
        max,
        weightedTemperature / temperatureWeight,
        humidityWeight == 0 ? null : weightedHumidity / humidityWeight,
        samples,
        batches.size(),
        underMaintenance,
        doorOpenSeconds,
        lastReceivedAt);
  }

  private static Double celsiusOf(Temperature temperature) {
    return temperature == null ? null : temperature.celsius();
  }
}
