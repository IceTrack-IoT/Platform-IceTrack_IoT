package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class MonitoringValueObjectsTest {

  @ParameterizedTest(name = "[{index}] {0} °C")
  @ValueSource(doubles = {-50.0, 0.0, 60.0})
  @DisplayName("Temperature(Double) -> accepts values inside the plausible range, limits included (AAA)")
  void temperatureAcceptsPlausible(double celsius) {
    assertThat(new Temperature(celsius).celsius()).isEqualTo(celsius);
  }

  @ParameterizedTest(name = "[{index}] {0} °C")
  @ValueSource(doubles = {-50.1, 60.1, 1000})
  @DisplayName("Temperature(Double) -> rejects implausible values (AAA)")
  void temperatureRejectsImplausible(double celsius) {
    var ex = assertThrows(IllegalArgumentException.class, () -> new Temperature(celsius));
    assertThat(ex.getMessage()).startsWith("Temperature out of plausible range");
  }

  @Test
  @DisplayName("Temperature(Double) -> a null value is allowed (AAA)")
  void temperatureAllowsNull() {
    assertThat(new Temperature(null).celsius()).isNull();
  }

  @Test
  @DisplayName("Temperature.isAbove(Temperature) / isBelow(Temperature) -> compare strictly and are false with nulls (AAA)")
  void temperatureComparisons() {
    var low = new Temperature(4.0);
    var high = new Temperature(8.0);
    var none = new Temperature(null);

    assertThat(high.isAbove(low)).isTrue();
    assertThat(low.isAbove(high)).isFalse();
    assertThat(low.isAbove(new Temperature(4.0))).isFalse();
    assertThat(low.isBelow(high)).isTrue();
    assertThat(high.isBelow(low)).isFalse();
    assertThat(low.isBelow(new Temperature(4.0))).isFalse();
    assertThat(none.isAbove(low)).isFalse();
    assertThat(low.isAbove(none)).isFalse();
    assertThat(none.isBelow(low)).isFalse();
    assertThat(low.isBelow(none)).isFalse();
  }

  @ParameterizedTest(name = "[{index}] {0} %")
  @ValueSource(doubles = {0.0, 55.5, 100.0})
  @DisplayName("Humidity(Double) -> accepts percentages from 0 to 100 (AAA)")
  void humidityAcceptsPercentages(double value) {
    assertThat(new Humidity(value).percentage()).isEqualTo(value);
  }

  @ParameterizedTest(name = "[{index}] {0} %")
  @ValueSource(doubles = {-0.1, 100.1})
  @DisplayName("Humidity(Double) -> rejects values outside 0–100 (AAA)")
  void humidityRejectsOutOfRange(double value) {
    assertThrows(IllegalArgumentException.class, () -> new Humidity(value));
  }

  @Test
  @DisplayName("Humidity(Double) -> a null value is allowed (AAA)")
  void humidityAllowsNull() {
    assertThat(new Humidity(null).percentage()).isNull();
  }

  @Test
  @DisplayName("EnvironmentObservation.UNAVAILABLE -> has no readings at all (AAA)")
  void environmentUnavailable() {
    assertThat(EnvironmentObservation.UNAVAILABLE)
        .isEqualTo(new EnvironmentObservation(null, null, null));
  }

  @ParameterizedTest(name = "[{index}] light={0}")
  @ValueSource(doubles = {-0.1, 100.1, Double.NaN, Double.POSITIVE_INFINITY})
  @DisplayName("EnvironmentObservation(...) -> rejects a light level outside 0–100 or not finite (AAA)")
  void environmentRejectsBadLight(double light) {
    assertThrows(IllegalArgumentException.class, () -> new EnvironmentObservation(light, 0, null));
  }

  @Test
  @DisplayName("EnvironmentObservation(...) -> accepts limits and rejects negative door-open seconds (AAA)")
  void environmentLimitsAndDoor() {
    assertThat(new EnvironmentObservation(0.0, 0, false).doorOpenSeconds()).isZero();
    assertThat(new EnvironmentObservation(100.0, 5, true).lightLevelMax()).isEqualTo(100.0);
    assertThrows(IllegalArgumentException.class, () -> new EnvironmentObservation(1.0, -1, null));
  }

  @Test
  @DisplayName("AlertSeverity.fromValue(int) -> resolves by numeric value and rejects unknown ones (AAA)")
  void severityFromValue() {
    assertThat(AlertSeverity.fromValue(1)).isEqualTo(AlertSeverity.INFO);
    assertThat(AlertSeverity.fromValue(2)).isEqualTo(AlertSeverity.WARNING);
    assertThat(AlertSeverity.fromValue(3)).isEqualTo(AlertSeverity.CRITICAL);
    var ex = assertThrows(IllegalArgumentException.class, () -> AlertSeverity.fromValue(9));
    assertThat(ex).hasMessage("Invalid AlertSeverity value: 9");
  }

  @Test
  @DisplayName("AlertType.fromValue(int) -> resolves by numeric value and rejects unknown ones (AAA)")
  void typeFromValue() {
    assertThat(AlertType.fromValue(1)).isEqualTo(AlertType.TEMPERATURE_EXCURSION);
    assertThat(AlertType.fromValue(2)).isEqualTo(AlertType.DEVICE_OFFLINE);
    assertThat(AlertType.TEMPERATURE_EXCURSION.getValue()).isEqualTo(1);
    assertThrows(IllegalArgumentException.class, () -> AlertType.fromValue(0));
  }

  @Test
  @DisplayName("AlertStatus.fromValue(int) -> resolves by ordinal and rejects unknown ones (AAA)")
  void statusFromValue() {
    assertThat(AlertStatus.fromValue(0)).isEqualTo(AlertStatus.OPEN);
    assertThat(AlertStatus.fromValue(1)).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(AlertStatus.fromValue(2)).isEqualTo(AlertStatus.RESOLVED);
    assertThat(AlertStatus.fromValue(3)).isEqualTo(AlertStatus.DISMISSED);
    assertThrows(IllegalArgumentException.class, () -> AlertStatus.fromValue(4));
    assertThrows(IllegalArgumentException.class, () -> AlertStatus.fromValue(-1));
  }

  @Test
  @DisplayName("AlertStatus.getValue() -> exposes the persisted numeric value (AAA)")
  void statusValues() {
    assertThat(AlertStatus.OPEN.getValue()).isEqualTo(1);
    assertThat(AlertStatus.DISMISSED.getValue()).isEqualTo(4);
  }

  @Test
  @DisplayName("MaintenanceOrigin -> APP lasts 2 hours and BUTTON 30 minutes (AAA)")
  void originLifetimes() {
    assertThat(MaintenanceOrigin.APP.lifetime()).isEqualTo(Duration.ofHours(2));
    assertThat(MaintenanceOrigin.BUTTON.lifetime()).isEqualTo(Duration.ofMinutes(30));
    assertThat(MaintenanceOrigin.BUTTON.expiryFrom(T0)).isEqualTo(T0.plus(Duration.ofMinutes(30)));
  }

  @Test
  @DisplayName("MaintenanceOrigin.expiryFrom(Instant) -> rejects a null start (AAA)")
  void originExpiryRequiresStart() {
    assertThrows(NullPointerException.class, () -> MaintenanceOrigin.APP.expiryFrom(null));
  }

  @ParameterizedTest(name = "[{index}] {0}")
  @CsvSource({"1m,ONE_MINUTE", "5m,FIVE_MINUTES", "15m,FIFTEEN_MINUTES", "1h,ONE_HOUR", "6h,SIX_HOURS"})
  @DisplayName("TelemetryBucketSize.fromLabel(String) -> resolves each known label (AAA)")
  void bucketSizeFromLabel(String label, TelemetryBucketSize expected) {
    assertThat(TelemetryBucketSize.fromLabel(label)).contains(expected);
    assertThat(expected.label()).isEqualTo(label);
  }

  @Test
  @DisplayName("TelemetryBucketSize.fromLabel(String) -> unknown or null labels are empty (AAA)")
  void bucketSizeUnknownLabel() {
    assertThat(TelemetryBucketSize.fromLabel("2m")).isEmpty();
    assertThat(TelemetryBucketSize.fromLabel(null)).isEmpty();
  }

  @Test
  @DisplayName("TelemetryBucketSize.width() -> matches the label (AAA)")
  void bucketSizeWidths() {
    assertThat(TelemetryBucketSize.ONE_MINUTE.width()).isEqualTo(Duration.ofMinutes(1));
    assertThat(TelemetryBucketSize.SIX_HOURS.width()).isEqualTo(Duration.ofHours(6));
  }

  @Test
  @DisplayName("TelemetryBucketSize.bucketStartOf(Instant) -> aligns the instant to the bucket boundary (AAA)")
  void bucketStartAlignsToBoundary() {
    var instant = Instant.parse("2026-01-01T10:07:45Z");

    assertThat(TelemetryBucketSize.FIVE_MINUTES.bucketStartOf(instant)).isEqualTo(Instant.parse("2026-01-01T10:05:00Z"));
    assertThat(TelemetryBucketSize.ONE_HOUR.bucketStartOf(instant)).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
    assertThat(TelemetryBucketSize.SIX_HOURS.bucketStartOf(instant)).isEqualTo(Instant.parse("2026-01-01T06:00:00Z"));
  }

  @Test
  @DisplayName("TelemetryBucketSize.bucketStartOf(Instant) -> aligns instants before the epoch downwards (AAA)")
  void bucketStartBeforeEpoch() {
    var instant = Instant.parse("1969-12-31T23:59:30Z");

    assertThat(TelemetryBucketSize.ONE_MINUTE.bucketStartOf(instant)).isEqualTo(Instant.parse("1969-12-31T23:59:00Z"));
  }

  @Test
  @DisplayName("TelemetryBucketSize.bucketCount(Instant, Instant) -> counts every bucket touched, both ends included (AAA)")
  void bucketCountIncludesBothEnds() {
    var from = Instant.parse("2026-01-01T10:00:30Z");
    var to = Instant.parse("2026-01-01T10:02:10Z");

    assertThat(TelemetryBucketSize.ONE_MINUTE.bucketCount(from, to)).isEqualTo(3);
    assertThat(TelemetryBucketSize.ONE_MINUTE.bucketCount(from, from)).isEqualTo(1);
  }

  @Test
  @DisplayName("TelemetryBucketSize.bucketCount(Instant, Instant) -> requires both instants (AAA)")
  void bucketCountRequiresInstants() {
    assertThrows(NullPointerException.class, () -> TelemetryBucketSize.ONE_HOUR.bucketCount(null, T0));
    assertThrows(NullPointerException.class, () -> TelemetryBucketSize.ONE_HOUR.bucketCount(T0, null));
  }

  @Test
  @DisplayName("TelemetryBucketSize.smallestFitting(...) -> picks the narrowest bucket within the point budget (AAA)")
  void smallestFittingPicksNarrowest() {
    var from = Instant.parse("2026-01-01T00:00:00Z");

    assertThat(TelemetryBucketSize.smallestFitting(from, from.plus(Duration.ofMinutes(30)), 500))
        .isEqualTo(TelemetryBucketSize.ONE_MINUTE);
    assertThat(TelemetryBucketSize.smallestFitting(from, from.plus(Duration.ofHours(24)), 500))
        .isEqualTo(TelemetryBucketSize.FIVE_MINUTES);
    assertThat(TelemetryBucketSize.smallestFitting(from, from.plus(Duration.ofDays(7)), 500))
        .isEqualTo(TelemetryBucketSize.ONE_HOUR);
  }

  @Test
  @DisplayName("TelemetryBucketSize.smallestFitting(...) -> falls back to SIX_HOURS when nothing fits (AAA)")
  void smallestFittingFallsBack() {
    var from = Instant.parse("2026-01-01T00:00:00Z");

    assertThat(TelemetryBucketSize.smallestFitting(from, from.plus(Duration.ofDays(365)), 10))
        .isEqualTo(TelemetryBucketSize.SIX_HOURS);
  }

  @Test
  @DisplayName("AlertPage(...) -> keeps an immutable copy of the content (AAA)")
  void alertPageCopiesContent() {
    // Arrange
    var content = new java.util.ArrayList<>(List.of(persistedTemperatureAlert()));

    // Act
    var page = new AlertPage(content, 0, 10, 1);
    content.clear();

    // Assert
    assertThat(page.content()).hasSize(1);
    assertThrows(UnsupportedOperationException.class, () -> page.content().clear());
  }

  @Test
  @DisplayName("AlertPage(...) -> rejects null content, negative page and non-positive size (AAA)")
  void alertPageValidation() {
    assertThrows(IllegalArgumentException.class, () -> new AlertPage(null, 0, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> new AlertPage(List.of(), -1, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> new AlertPage(List.of(), 0, 0, 0));
  }

  @Test
  @DisplayName("AlertPage.empty(int, int) -> an empty page with zero total (AAA)")
  void alertPageEmpty() {
    var page = AlertPage.empty(2, 20);

    assertThat(page.content()).isEmpty();
    assertThat(page.page()).isEqualTo(2);
    assertThat(page.size()).isEqualTo(20);
    assertThat(page.totalElements()).isZero();
  }

  @ParameterizedTest(name = "[{index}] total={0}, size={1} -> {2} pages")
  @CsvSource({"0,10,1", "1,10,1", "10,10,1", "11,10,2", "95,10,10", "101,100,2"})
  @DisplayName("AlertPage.totalPages() -> rounds up and is never below one (AAA)")
  void alertPageTotalPages(long total, int size, int expectedPages) {
    assertThat(new AlertPage(List.of(), 0, size, total).totalPages()).isEqualTo(expectedPages);
  }

  @Test
  @DisplayName("TelemetryBucketSeries(...) -> requires the bucket size and normalizes null buckets to empty (AAA)")
  void seriesValidation() {
    assertThrows(NullPointerException.class, () -> new TelemetryBucketSeries(null, List.of()));
    assertThat(new TelemetryBucketSeries(TelemetryBucketSize.ONE_HOUR, null).buckets()).isEmpty();
  }

  @Test
  @DisplayName("ApplicableAlertPolicy(...) -> requires equipment and policy and exposes whether it is the default (AAA)")
  void applicablePolicy() {
    assertThrows(NullPointerException.class, () -> new ApplicableAlertPolicy(null, equipmentPolicy()));
    assertThrows(NullPointerException.class, () -> new ApplicableAlertPolicy(EQUIPMENT_ID, null));
    assertThat(new ApplicableAlertPolicy(EQUIPMENT_ID, equipmentPolicy()).isPlatformDefault()).isFalse();
    assertThat(new ApplicableAlertPolicy(EQUIPMENT_ID,
        pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy.platformDefault())
        .isPlatformDefault()).isTrue();
  }
}
