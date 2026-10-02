package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import java.time.LocalDateTime;

/** Outbound REST resource representing an alert. */
public record AlertResource(
    Long id,
    Long equipmentId,
    String type,
    String severity,
    String status,
    Double peakTemperature,
    LocalDateTime openedAt,
    LocalDateTime resolvedAt) {
}
