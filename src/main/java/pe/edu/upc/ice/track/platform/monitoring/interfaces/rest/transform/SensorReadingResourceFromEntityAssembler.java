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
    return new SensorReadingResource(
        reading.getId(), reading.getEquipmentId().equipmentId(), reading.getDeviceId(),
        reading.getMinTemperature().celsius(), reading.getMaxTemperature().celsius(),
        reading.getAvgTemperature().celsius(), reading.getHumidity().percentage(),
        reading.getRecordedAt());
  }
}
