package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.SensorReadingPersistenceEntity;

/**
 * Bidirectional mapper between {@link SensorReading} and {@link SensorReadingPersistenceEntity}.
 *
 * <p>This is the boundary where the {@code Temperature}/{@code Humidity} Value Objects are
 * unwrapped to plain {@code Double} columns and re-wrapped on the way back — the persistence
 * entity itself never holds a Value Object, only primitives, matching how {@code Profile}
 * unwraps {@code Email}/{@code Phone} at its own persistence boundary.</p>
 */
public final class SensorReadingPersistenceAssembler {

  private SensorReadingPersistenceAssembler() {
  }

  public static SensorReadingPersistenceEntity toPersistenceEntityFromDomain(SensorReading reading) {
    var entity = new SensorReadingPersistenceEntity();
    entity.setId(reading.getId());
    entity.setReadingUid(reading.getReadingUid());
    entity.setEquipmentId(reading.getEquipmentId().equipmentId());
    entity.setDeviceId(reading.getDeviceId());
    entity.setMinTemperature(reading.getMinTemperature().celsius());
    entity.setMaxTemperature(reading.getMaxTemperature().celsius());
    entity.setAvgTemperature(reading.getAvgTemperature().celsius());
    entity.setHumidity(reading.getHumidity().percentage());
    entity.setSampleCount(reading.getSampleCount());
    entity.setRecordedAt(reading.getRecordedAt());
    entity.setReceivedAt(reading.getReceivedAt());
    return entity;
  }

  public static SensorReading toDomainFromPersistenceEntity(SensorReadingPersistenceEntity entity) {
    var reading = new SensorReading(
        entity.getReadingUid(),new EquipmentId(entity.getEquipmentId()), entity.getDeviceId(),
        entity.getMinTemperature(), entity.getMaxTemperature(), entity.getAvgTemperature(),
        entity.getHumidity(), entity.getSampleCount(), entity.getRecordedAt());
    reading.assignId(entity.getId());
    return reading;
  }
}
