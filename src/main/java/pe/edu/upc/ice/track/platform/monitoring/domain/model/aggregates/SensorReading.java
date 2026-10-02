package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Humidity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.Temperature;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * SensorReading aggregate root of the {@code monitoring} bounded context.
 *
 * <p>Represents one aggregated batch of samples taken by a device over a short window: the
 * minimum, maximum and average temperature observed, the humidity, and how many raw samples were
 * folded into it. Immutable once created and identified by a client-generated {@code readingUid}
 * so that a retried delivery from the edge never creates a duplicate row.</p>
 *
 * <p>Temperature and humidity are held as {@link Temperature} and {@link Humidity} Value
 * Objects, not raw {@code Double}, so an out-of-physical-range value can never enter this
 * aggregate silently — the same guarantee {@code Email}/{@code Phone} give {@code Profile}.</p>
 *
 * <p>Referenced from {@link Alert} by identifier only ({@code triggeringReadingId}), never by
 * object composition, so that loading an alert never forces loading the reading that raised it.</p>
 */
@Getter
public class SensorReading extends AbstractDomainAggregateRoot<SensorReading> {

  private Long id;
  private final UUID readingUid;
  private EquipmentId equipmentId;
  private final Long deviceId;
  private final Temperature minTemperature;
  private final Temperature maxTemperature;
  private final Temperature avgTemperature;
  private final Humidity humidity;
  private final Integer sampleCount;
  private final LocalDateTime recordedAt;
  private final LocalDateTime receivedAt;

  public SensorReading(UUID readingUid, EquipmentId equipmentId, Long deviceId, Double minTemperature,
                       Double maxTemperature, Double avgTemperature, Double humidity,
                       Integer sampleCount, LocalDateTime recordedAt) {
    this.readingUid = Objects.requireNonNull(readingUid, "readingUid must not be null");
    this.equipmentId = Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    this.deviceId = Objects.requireNonNull(deviceId, "deviceId must not be null");
    this.minTemperature = new Temperature(minTemperature);
    this.maxTemperature = new Temperature(maxTemperature);
    this.avgTemperature = new Temperature(avgTemperature);
    this.humidity = new Humidity(humidity);
    this.sampleCount = sampleCount;
    this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt must not be null");
    this.receivedAt = LocalDateTime.now();
  }

  public void assignId(Long id) {
    this.id = id;
  }
}
