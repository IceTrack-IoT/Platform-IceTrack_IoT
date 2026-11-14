package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.ReadingRecordedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EnvironmentObservation;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class SensorReadingTest {

  private static final EquipmentId EQUIPMENT = new EquipmentId(EQUIPMENT_ID);

  /** Records a reading overriding the calibrated and raw statistics; everything else is valid. */
  private static SensorReading record(double min, double avg, double max,
                                      double rawMin, double rawAvg, double rawMax,
                                      Double humidity, Integer samples, Long configVersion) {
    return SensorReading.record(READING_UID, EQUIPMENT, DEVICE_ID, min, max, avg, rawMin, rawMax, rawAvg,
        humidity, samples, null, T0, configVersion, false);
  }

  @Test
  @DisplayName("record(...) -> builds a reading and registers ReadingRecordedEvent (AAA)")
  void recordBuildsReadingAndEvent() {
    // Act
    var reading = sensorReading(T0, 4.0, 5.0, 6.0, 40, true);

    // Assert
    assertThat(reading.getId()).isNull();
    assertThat(reading.getReadingUid()).isEqualTo(READING_UID);
    assertThat(reading.getEquipmentId()).isEqualTo(EQUIPMENT);
    assertThat(reading.getDeviceId()).isEqualTo(DEVICE_ID);
    assertThat(reading.getMinTemperature().celsius()).isEqualTo(4.0);
    assertThat(reading.getAvgTemperature().celsius()).isEqualTo(5.0);
    assertThat(reading.getMaxTemperature().celsius()).isEqualTo(6.0);
    assertThat(reading.getHumidity().percentage()).isEqualTo(55.0);
    assertThat(reading.getSampleCount()).isEqualTo(40);
    assertThat(reading.getRecordedAt()).isEqualTo(T0);
    assertThat(reading.getAppliedConfigVersion()).isEqualTo(1L);
    assertThat(reading.isUnderMaintenance()).isTrue();
    assertThat(reading.getReceivedAt()).isNotNull();
    assertThat(reading.domainEvents()).hasSize(1);
    var event = (ReadingRecordedEvent) reading.domainEvents().iterator().next();
    assertThat(event.readingUid()).isEqualTo(READING_UID);
    assertThat(event.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(event.avgTemperature()).isEqualTo(5.0);
    assertThat(event.sampleCount()).isEqualTo(40);
    assertThat(event.underMaintenance()).isTrue();
  }

  @Test
  @DisplayName("record(...) -> keeps the supplied environment observation (AAA)")
  void recordKeepsEnvironment() {
    var reading = sensorReading();

    assertThat(reading.getEnvironment()).isEqualTo(new EnvironmentObservation(10.0, 12, true));
  }

  @Test
  @DisplayName("record(...) -> a missing environment becomes UNAVAILABLE (AAA)")
  void recordDefaultsEnvironment() {
    var reading = record(4, 5, 6, 4, 5, 6, 50.0, 10, 0L);

    assertThat(reading.getEnvironment()).isSameAs(EnvironmentObservation.UNAVAILABLE);
  }

  @Test
  @DisplayName("record(...) -> accepts a calibrated value exactly 5 °C from its raw value (AAA)")
  void recordAcceptsMaximumCalibrationOffset() {
    var reading = record(9, 10, 11, 4, 5, 6, 50.0, 10, 0L);

    assertThat(reading.getMinTemperature().celsius()).isEqualTo(9.0);
  }

  @Test
  @DisplayName("record(...) -> rejects a calibrated value more than 5 °C from its raw value (AAA)")
  void recordRejectsExcessiveCalibration() {
    var ex = assertThrows(IllegalArgumentException.class, () -> record(9.5, 10, 11, 4, 5, 6, 50.0, 10, 0L));
    assertThat(ex.getMessage()).startsWith("minTemperature must be within 5.0");
  }

  @ParameterizedTest(name = "[{index}] raw={0}")
  @ValueSource(doubles = {-0.1, 50.1})
  @DisplayName("record(...) -> rejects a raw temperature outside the sensor range 0–50 °C (AAA)")
  void recordRejectsRawOutsideSensorRange(double raw) {
    var ex = assertThrows(IllegalArgumentException.class, () -> record(raw, raw, raw, raw, raw, raw, 50.0, 10, 0L));
    assertThat(ex.getMessage()).startsWith("rawMinTemperature must be between 0.0 and 50.0");
  }

  @Test
  @DisplayName("record(...) -> rejects raw temperatures that are not ordered min <= avg <= max (AAA)")
  void recordRejectsUnorderedRaw() {
    var ex = assertThrows(IllegalArgumentException.class, () -> record(4, 5, 6, 6, 5, 4, 50.0, 10, 0L));
    assertThat(ex.getMessage()).startsWith("Raw temperatures must satisfy min <= avg <= max");
  }

  @Test
  @DisplayName("record(...) -> rejects calibrated temperatures that are not ordered (AAA)")
  void recordRejectsUnorderedCalibrated() {
    var ex = assertThrows(IllegalArgumentException.class, () -> record(6, 5, 4, 4, 5, 6, 50.0, 10, 0L));
    assertThat(ex.getMessage()).startsWith("Temperatures must satisfy min <= avg <= max");
  }

  @ParameterizedTest(name = "[{index}] humidity={0}")
  @ValueSource(doubles = {19.9, 90.1, Double.NaN})
  @DisplayName("record(...) -> rejects humidity outside the sensor range 20–90 % (AAA)")
  void recordRejectsHumidityOutsideRange(double humidity) {
    assertThrows(IllegalArgumentException.class, () -> record(4, 5, 6, 4, 5, 6, humidity, 10, 0L));
  }

  @Test
  @DisplayName("record(...) -> accepts humidity exactly at the range limits (AAA)")
  void recordAcceptsHumidityLimits() {
    assertThat(record(4, 5, 6, 4, 5, 6, 20.0, 10, 0L).getHumidity().percentage()).isEqualTo(20.0);
    assertThat(record(4, 5, 6, 4, 5, 6, 90.0, 10, 0L).getHumidity().percentage()).isEqualTo(90.0);
  }

  @Test
  @DisplayName("record(...) -> requires humidity, sample count and applied config version (AAA)")
  void recordRequiresMandatoryNumbers() {
    assertThrows(NullPointerException.class, () -> record(4, 5, 6, 4, 5, 6, null, 10, 0L));
    assertThrows(NullPointerException.class, () -> record(4, 5, 6, 4, 5, 6, 50.0, null, 0L));
    assertThrows(NullPointerException.class, () -> record(4, 5, 6, 4, 5, 6, 50.0, 10, null));
  }

  @Test
  @DisplayName("record(...) -> rejects a sample count below one and a negative config version (AAA)")
  void recordRejectsInvalidCounts() {
    assertThrows(IllegalArgumentException.class, () -> record(4, 5, 6, 4, 5, 6, 50.0, 0, 0L));
    assertThrows(IllegalArgumentException.class, () -> record(4, 5, 6, 4, 5, 6, 50.0, 10, -1L));
  }

  @Test
  @DisplayName("record(...) -> requires reading uid, equipment, device and recording instant (AAA)")
  void recordRequiresIdentity() {
    assertThrows(NullPointerException.class, () -> SensorReading.record(null, EQUIPMENT, DEVICE_ID,
        4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 50.0, 10, null, T0, 0L, false));
    assertThrows(NullPointerException.class, () -> SensorReading.record(UUID.randomUUID(), null, DEVICE_ID,
        4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 50.0, 10, null, T0, 0L, false));
    assertThrows(NullPointerException.class, () -> SensorReading.record(UUID.randomUUID(), EQUIPMENT, null,
        4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 50.0, 10, null, T0, 0L, false));
    assertThrows(NullPointerException.class, () -> SensorReading.record(UUID.randomUUID(), EQUIPMENT, DEVICE_ID,
        4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 50.0, 10, null, null, 0L, false));
  }

  @Test
  @DisplayName("SensorReading(...) -> a null reception instant defaults to now (AAA)")
  void constructorDefaultsReceivedAt() {
    var before = Instant.now();

    var reading = new SensorReading(READING_UID, EQUIPMENT, DEVICE_ID, 4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 50.0, 10,
        null, T0, null, 0L, false);

    assertThat(reading.getReceivedAt()).isBetween(before, Instant.now());
  }

  @Test
  @DisplayName("assignId(Long) -> stores the persistence id (AAA)")
  void assignId() {
    var reading = sensorReading();

    reading.assignId(5L);

    assertThat(reading.getId()).isEqualTo(5L);
  }
}
