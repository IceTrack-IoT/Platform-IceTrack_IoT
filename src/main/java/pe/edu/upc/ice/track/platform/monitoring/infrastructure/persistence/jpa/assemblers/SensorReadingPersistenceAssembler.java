package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EnvironmentObservation;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities.SensorReadingPersistenceEntity;

/**
 * Bidirectional mapper between {@link SensorReading} and {@link SensorReadingPersistenceEntity}.
 *
 * <p>This is the boundary where the {@code Temperature}/{@code Humidity} Value Objects and the
 * {@link EnvironmentObservation} are unwrapped to plain columns and re-wrapped on the way back -
 * the persistence entity itself never holds a Value Object, only primitives.</p>
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
    entity.setRawMinTemperature(reading.getRawMinTemperature().celsius());
    entity.setRawMaxTemperature(reading.getRawMaxTemperature().celsius());
    entity.setRawAvgTemperature(reading.getRawAvgTemperature().celsius());
    entity.setHumidity(reading.getHumidity().percentage());
    entity.setSampleCount(reading.getSampleCount());
    entity.setLightLevelMax(reading.getEnvironment().lightLevelMax());
    entity.setDoorOpenSeconds(reading.getEnvironment().doorOpenSeconds());
    entity.setPresenceDetected(reading.getEnvironment().presenceDetected());
    entity.setRecordedAt(reading.getRecordedAt());
    entity.setReceivedAt(reading.getReceivedAt());
    entity.setAppliedConfigVersion(reading.getAppliedConfigVersion());
    return entity;
  }

  public static SensorReading toDomainFromPersistenceEntity(SensorReadingPersistenceEntity entity) {
    var reading = new SensorReading(
        entity.getReadingUid(), new EquipmentId(entity.getEquipmentId()), entity.getDeviceId(),
        entity.getMinTemperature(), entity.getMaxTemperature(), entity.getAvgTemperature(),
        entity.getRawMinTemperature(), entity.getRawMaxTemperature(), entity.getRawAvgTemperature(),
        entity.getHumidity(), entity.getSampleCount(),
        new EnvironmentObservation(entity.getLightLevelMax(), entity.getDoorOpenSeconds(), entity.getPresenceDetected()),
        entity.getRecordedAt(), entity.getReceivedAt(), entity.getAppliedConfigVersion());
    reading.assignId(entity.getId());
    return reading;
  }
}
