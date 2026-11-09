package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.ReadingRecordedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EnvironmentObservation;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Humidity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * SensorReading aggregate root of the {@code monitoring} bounded context.
 *
 * <p>Represents one aggregated batch of samples taken by a device over a short window: the
 * minimum, maximum and average temperature observed, the humidity, how many raw samples were
 * folded into it, and what the light and PIR sensors observed. Immutable once created and
 * identified by a client-generated {@code readingUid} so that a retried delivery from the edge
 * never creates a duplicate row.</p>
 *
 * <p>Temperatures come in two forms. The <em>raw</em> values are what the DHT11 probe measured.
 * The <em>calibrated</em> values ({@code minTemperature}, {@code maxTemperature},
 * {@code avgTemperature}) are the raw ones corrected by the calibration offset the edge applied,
 * and are what thresholds are evaluated against. Both are kept, so a calibration can always be
 * audited or rolled back.</p>
 *
 * <p>A new batch enters through {@link #record}, which enforces:</p>
 * <ul>
 *   <li>raw temperatures within the probe range, {@value #SENSOR_MIN_CELSIUS} to
 *       {@value #SENSOR_MAX_CELSIUS} °C, and relative humidity within
 *       {@value #SENSOR_MIN_HUMIDITY} to {@value #SENSOR_MAX_HUMIDITY} %; a value outside them is a
 *       sensor fault or a forged payload, never a measurement;</li>
 *   <li>{@code min <= avg <= max}, for the raw and for the calibrated values;</li>
 *   <li>each calibrated value within {@value #MAX_CALIBRATION_OFFSET_CELSIUS} °C of its raw
 *       value, the largest offset an owner can configure;</li>
 *   <li>a positive sample count and a non-negative applied configuration version.</li>
 * </ul>
 *
 * <p>The public constructor only reconstitutes stored rows and keeps the wider plausibility checks
 * of the {@link Temperature} and {@link Humidity} Value Objects.</p>
 *
 * <p>Referenced from {@link Alert} by identifier only ({@code triggeringReadingId}), never by
 * object composition, so that loading an alert never forces loading the reading that raised it.</p>
 */
@Getter
public class SensorReading extends AbstractDomainAggregateRoot<SensorReading> {

  /** Lowest temperature the DHT11 probe can measure. */
  public static final double SENSOR_MIN_CELSIUS = 0.0;
  /** Highest temperature the DHT11 probe can measure. */
  public static final double SENSOR_MAX_CELSIUS = 50.0;
  /** Lowest relative humidity the DHT11 probe can measure. */
  public static final double SENSOR_MIN_HUMIDITY = 20.0;
  /** Highest relative humidity the DHT11 probe can measure. */
  public static final double SENSOR_MAX_HUMIDITY = 90.0;
  /** Largest calibration offset, in either direction, an owner can configure. */
  public static final double MAX_CALIBRATION_OFFSET_CELSIUS = 5.0;

  private Long id;
  private final UUID readingUid;
  private EquipmentId equipmentId;
  private final Long deviceId;
  private final Temperature minTemperature;
  private final Temperature maxTemperature;
  private final Temperature avgTemperature;
  private final Temperature rawMinTemperature;
  private final Temperature rawMaxTemperature;
  private final Temperature rawAvgTemperature;
  private final Humidity humidity;
  private final Integer sampleCount;
  private final EnvironmentObservation environment;
  private final Instant recordedAt;
  private final Instant receivedAt;
  private final Long appliedConfigVersion;

  /**
   * Whether a maintenance window covered {@code recordedAt}. Such a reading is stored and shown like
   * any other, but it never changes an alert: the unit was being worked on.
   */
  private final boolean underMaintenance;

  /**
   * Reconstitutes a stored reading. Raw temperatures, the applied configuration version and every
   * environment value may be {@code null} on rows stored before they were tracked.
   */
  public SensorReading(UUID readingUid, EquipmentId equipmentId, Long deviceId,
                       Double minTemperature, Double maxTemperature, Double avgTemperature,
                       Double rawMinTemperature, Double rawMaxTemperature, Double rawAvgTemperature,
                       Double humidity, Integer sampleCount, EnvironmentObservation environment,
                       Instant recordedAt, Instant receivedAt, Long appliedConfigVersion,
                       boolean underMaintenance) {
    this.readingUid = Objects.requireNonNull(readingUid, "readingUid must not be null");
    this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    this.deviceId = Objects.requireNonNull(deviceId, "deviceId must not be null");
    this.minTemperature = new Temperature(minTemperature);
    this.maxTemperature = new Temperature(maxTemperature);
    this.avgTemperature = new Temperature(avgTemperature);
    this.rawMinTemperature = new Temperature(rawMinTemperature);
    this.rawMaxTemperature = new Temperature(rawMaxTemperature);
    this.rawAvgTemperature = new Temperature(rawAvgTemperature);
    this.humidity = new Humidity(humidity);
    this.sampleCount = sampleCount;
    this.environment = environment == null ? EnvironmentObservation.UNAVAILABLE : environment;
    this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    this.receivedAt = receivedAt == null ? Instant.now() : receivedAt;
    this.appliedConfigVersion = appliedConfigVersion;
    this.underMaintenance = underMaintenance;
  }

  /**
   * Creates a newly received batch, enforcing the physical bounds of the sensor and the
   * consistency of its calibration.
   *
   * <p>Registers a {@link ReadingRecordedEvent}, which the repository publishes once the batch is
   * saved, so the batch can be streamed to the owner's dashboards after the transaction commits.</p>
   *
   * @param environment      what the light and PIR sensors observed; {@code null} when neither
   *                         reported
   * @param underMaintenance whether a maintenance window of the unit covers {@code recordedAt}
   * @throws NullPointerException     when a required value is missing
   * @throws IllegalArgumentException when a rule listed on this class is broken
   */
  public static SensorReading record(UUID readingUid, EquipmentId equipmentId, Long deviceId,
                                     Double minTemperature, Double maxTemperature, Double avgTemperature,
                                     Double rawMinTemperature, Double rawMaxTemperature, Double rawAvgTemperature,
                                     Double humidity, Integer sampleCount, EnvironmentObservation environment,
                                     Instant recordedAt, Long appliedConfigVersion, boolean underMaintenance) {
    requireRawInSensorRange("rawMinTemperature", rawMinTemperature);
    requireRawInSensorRange("rawMaxTemperature", rawMaxTemperature);
    requireRawInSensorRange("rawAvgTemperature", rawAvgTemperature);
    requireOrdered("Raw temperatures", rawMinTemperature, rawAvgTemperature, rawMaxTemperature);

    requireCalibratedFrom("minTemperature", minTemperature, rawMinTemperature);
    requireCalibratedFrom("maxTemperature", maxTemperature, rawMaxTemperature);
    requireCalibratedFrom("avgTemperature", avgTemperature, rawAvgTemperature);
    requireOrdered("Temperatures", minTemperature, avgTemperature, maxTemperature);

    Objects.requireNonNull(humidity, "humidity must not be null");
    if (!Double.isFinite(humidity) || humidity < SENSOR_MIN_HUMIDITY || humidity > SENSOR_MAX_HUMIDITY) {
      throw new IllegalArgumentException("humidity must be between %s and %s %%, was %s"
          .formatted(SENSOR_MIN_HUMIDITY, SENSOR_MAX_HUMIDITY, humidity));
    }
    Objects.requireNonNull(sampleCount, "sampleCount must not be null");
    if (sampleCount < 1) {
      throw new IllegalArgumentException("sampleCount must be at least 1, was " + sampleCount);
    }
    Objects.requireNonNull(appliedConfigVersion, "appliedConfigVersion must not be null");
    if (appliedConfigVersion < 0) {
      throw new IllegalArgumentException("appliedConfigVersion must not be negative, was " + appliedConfigVersion);
    }
    var reading = new SensorReading(readingUid, equipmentId, deviceId,
        minTemperature, maxTemperature, avgTemperature,
        rawMinTemperature, rawMaxTemperature, rawAvgTemperature,
        humidity, sampleCount, environment, recordedAt, Instant.now(), appliedConfigVersion, underMaintenance);
    reading.registerDomainEvent(new ReadingRecordedEvent(
        reading.readingUid, reading.equipmentId.equipmentId(), reading.deviceId,
        reading.minTemperature.celsius(), reading.maxTemperature.celsius(), reading.avgTemperature.celsius(),
        reading.humidity.percentage(), reading.sampleCount,
        reading.recordedAt, reading.receivedAt, reading.underMaintenance));
    return reading;
  }

  public void assignId(Long id) {
    this.id = id;
  }

  private static void requireRawInSensorRange(String field, Double celsius) {
    Objects.requireNonNull(celsius, field + " must not be null");
    if (!Double.isFinite(celsius) || celsius < SENSOR_MIN_CELSIUS || celsius > SENSOR_MAX_CELSIUS) {
      throw new IllegalArgumentException("%s must be between %s and %s °C, was %s"
          .formatted(field, SENSOR_MIN_CELSIUS, SENSOR_MAX_CELSIUS, celsius));
    }
  }

  private static void requireCalibratedFrom(String field, Double calibrated, Double raw) {
    Objects.requireNonNull(calibrated, field + " must not be null");
    if (!Double.isFinite(calibrated) || Math.abs(calibrated - raw) > MAX_CALIBRATION_OFFSET_CELSIUS) {
      throw new IllegalArgumentException("%s must be within %s °C of its raw value %s, was %s"
          .formatted(field, MAX_CALIBRATION_OFFSET_CELSIUS, raw, calibrated));
    }
  }

  private static void requireOrdered(String label, double min, double avg, double max) {
    if (min > avg || avg > max) {
      throw new IllegalArgumentException(
          "%s must satisfy min <= avg <= max, were %s, %s, %s".formatted(label, min, avg, max));
    }
  }
}
