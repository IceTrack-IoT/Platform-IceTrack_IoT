package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import java.time.LocalDateTime;

/** Outbound REST resource representing a stored sensor reading. */
public record SensorReadingResource(
    Long id,
    Long equipmentId,
    Long deviceId,
    Double minTemperature,
    Double maxTemperature,
    Double avgTemperature,
    Double humidity,
    LocalDateTime recordedAt) {
}
