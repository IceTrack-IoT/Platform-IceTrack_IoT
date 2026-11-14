package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class TelemetryBucketTest {

  private static final Instant START = Instant.parse("2026-01-01T10:00:00Z");

  private static SensorReading reading(Instant recordedAt, double min, double avg, double max, int samples,
                                       double humidity, Integer doorSeconds, boolean maintenance) {
    return SensorReading.record(UUID.randomUUID(), new EquipmentId(EQUIPMENT_ID), DEVICE_ID, min, max, avg, min,
        max, avg, humidity, samples, new EnvironmentObservation(null, doorSeconds, null), recordedAt, 1L,
        maintenance);
  }

  @Test
  @DisplayName("aggregate(...) -> null or empty readings give no buckets (AAA)")
  void emptyInputGivesNoBuckets() {
    assertThat(TelemetryBucket.aggregate(null, TelemetryBucketSize.ONE_MINUTE)).isEmpty();
    assertThat(TelemetryBucket.aggregate(java.util.List.of(), TelemetryBucketSize.ONE_MINUTE)).isEmpty();
  }

  @Test
  @DisplayName("aggregate(...) -> requires the bucket size (AAA)")
  void sizeRequired() {
    assertThrows(NullPointerException.class, () -> TelemetryBucket.aggregate(java.util.List.of(), null));
  }

  @Test
  @DisplayName("aggregate(...) -> a single reading becomes one bucket mirroring its statistics (AAA)")
  void singleReadingBucket() {
    // Arrange
    var batch = sensorReading(START.plusSeconds(20), 4.0, 5.0, 6.0, 40, false);

    // Act
    var buckets = TelemetryBucket.aggregate(java.util.List.of(batch), TelemetryBucketSize.FIVE_MINUTES);

    // Assert
    assertThat(buckets).hasSize(1);
    var bucket = buckets.get(0);
    assertThat(bucket.bucketStart()).isEqualTo(START);
    assertThat(bucket.bucketEnd()).isEqualTo(START.plusSeconds(300));
    assertThat(bucket.minTemperature()).isEqualTo(4.0);
    assertThat(bucket.maxTemperature()).isEqualTo(6.0);
    assertThat(bucket.avgTemperature()).isEqualTo(5.0);
    assertThat(bucket.avgHumidity()).isEqualTo(55.0);
    assertThat(bucket.sampleCount()).isEqualTo(40);
    assertThat(bucket.batchCount()).isEqualTo(1);
    assertThat(bucket.underMaintenance()).isFalse();
    assertThat(bucket.doorOpenSeconds()).isEqualTo(12L);
    assertThat(bucket.lastReceivedAt()).isEqualTo(batch.getReceivedAt());
  }

  @Test
  @DisplayName("aggregate(...) -> readings in one bucket are combined with the average weighted by sample count (AAA)")
  void combinesReadingsWithWeightedAverage() {
    // Arrange
    var first = reading(START.plusSeconds(10), 4.0, 5.0, 6.0, 10, 40.0, 5, false);
    var second = reading(START.plusSeconds(40), 3.0, 8.0, 9.0, 30, 60.0, 7, true);

    // Act
    var buckets = TelemetryBucket.aggregate(java.util.List.of(first, second), TelemetryBucketSize.ONE_MINUTE);

    // Assert
    assertThat(buckets).hasSize(1);
    var bucket = buckets.get(0);
    assertThat(bucket.minTemperature()).isEqualTo(3.0);
    assertThat(bucket.maxTemperature()).isEqualTo(9.0);
    assertThat(bucket.avgTemperature()).isCloseTo((5.0 * 10 + 8.0 * 30) / 40, within(1e-9));
    assertThat(bucket.avgHumidity()).isCloseTo((40.0 * 10 + 60.0 * 30) / 40, within(1e-9));
    assertThat(bucket.sampleCount()).isEqualTo(40);
    assertThat(bucket.batchCount()).isEqualTo(2);
    assertThat(bucket.underMaintenance()).isTrue();
    assertThat(bucket.doorOpenSeconds()).isEqualTo(12L);
  }

  @Test
  @DisplayName("aggregate(...) -> readings in different buckets are split and returned in chronological order (AAA)")
  void splitsAndSortsBuckets() {
    // Arrange (given out of order)
    var later = reading(START.plusSeconds(130), 4.0, 5.0, 6.0, 10, 50.0, null, false);
    var earlier = reading(START.plusSeconds(5), 4.0, 5.0, 6.0, 10, 50.0, null, false);

    // Act
    var buckets = TelemetryBucket.aggregate(java.util.List.of(later, earlier), TelemetryBucketSize.ONE_MINUTE);

    // Assert
    assertThat(buckets).extracting(TelemetryBucket::bucketStart)
        .containsExactly(START, START.plusSeconds(120));
  }

  @Test
  @DisplayName("aggregate(...) -> door-open seconds stay null when no reading reported them (AAA)")
  void doorSecondsNullWhenUnreported() {
    var batch = reading(START, 4.0, 5.0, 6.0, 10, 50.0, null, false);

    var bucket = TelemetryBucket.aggregate(java.util.List.of(batch), TelemetryBucketSize.ONE_HOUR).get(0);

    assertThat(bucket.doorOpenSeconds()).isNull();
  }

  @Test
  @DisplayName("aggregate(...) -> the latest reception instant of the bucket is reported (AAA)")
  void latestReceptionReported() {
    // Arrange
    var a = reading(START, 4.0, 5.0, 6.0, 10, 50.0, null, false);
    var b = reading(START.plusSeconds(1), 4.0, 5.0, 6.0, 10, 50.0, null, false);
    var latest = Arrays.asList(a, b).stream().map(SensorReading::getReceivedAt).max(Instant::compareTo).orElseThrow();

    // Act
    var bucket = TelemetryBucket.aggregate(java.util.List.of(a, b), TelemetryBucketSize.ONE_HOUR).get(0);

    // Assert
    assertThat(bucket.lastReceivedAt()).isEqualTo(latest);
  }

  @Test
  @DisplayName("aggregate(...) -> skips null readings and readings without a recording instant or average (AAA)")
  void skipsUnusableReadings() {
    // Arrange
    var good = reading(START, 4.0, 5.0, 6.0, 10, 50.0, null, false);
    var withoutAverage = new SensorReading(UUID.randomUUID(), new EquipmentId(EQUIPMENT_ID), DEVICE_ID,
        null, null, null, null, null, null, 50.0, 10, null, START, null, 0L, false);

    // Act
    var buckets = TelemetryBucket.aggregate(Arrays.asList(null, withoutAverage, good), TelemetryBucketSize.ONE_HOUR);

    // Assert
    assertThat(buckets).hasSize(1);
    assertThat(buckets.get(0).batchCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("aggregate(...) -> returns an immutable list (AAA)")
  void resultIsImmutable() {
    var buckets = TelemetryBucket.aggregate(java.util.List.of(sensorReading()), TelemetryBucketSize.ONE_HOUR);

    assertThrows(UnsupportedOperationException.class, buckets::clear);
  }
}
