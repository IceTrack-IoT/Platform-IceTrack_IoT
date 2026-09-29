package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

/** Inbound REST resource carrying one reading batch sent by the Edge API. */
public record RecordReadingBatchResource(
    UUID readingUid,
    Long equipmentId,
    Long deviceId,
    Double minTemperature,
    Double maxTemperature,
    Double avgTemperature,
    Double humidity,
    Integer sampleCount,
    LocalDateTime recordedAt) {
}
