package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.SensorReadingResource;

/**
 * Maps a {@link SensorReading} into a {@link SensorReadingResource}.
 *
 * <p>{@code Temperature}/{@code Humidity} are unwrapped to plain {@code Double} here — the REST
 * contract stays primitive, the Value Object guarantee only needs to hold inside the domain.</p>
 */
public final class SensorReadingResourceFromEntityAssembler {

  private SensorReadingResourceFromEntityAssembler() {
  }

  public static SensorReadingResource toResourceFromEntity(SensorReading reading) {
    var environment = reading.getEnvironment();
    return new SensorReadingResource(
        reading.getId(), reading.getReadingUid().toString(), reading.getEquipmentId().equipmentId(),
        reading.getDeviceId(),
        reading.getMinTemperature().celsius(), reading.getMaxTemperature().celsius(),
        reading.getAvgTemperature().celsius(),
        reading.getRawMinTemperature().celsius(), reading.getRawMaxTemperature().celsius(),
        reading.getRawAvgTemperature().celsius(),
        reading.getHumidity().percentage(), reading.getSampleCount(),
        environment.lightLevelMax(), environment.doorOpenSeconds(), environment.presenceDetected(),
        reading.getRecordedAt(), reading.getAppliedConfigVersion(), reading.isUnderMaintenance());
  }
}
