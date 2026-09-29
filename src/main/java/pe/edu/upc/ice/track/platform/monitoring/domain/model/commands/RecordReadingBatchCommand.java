package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command to record one aggregated sensor reading batch coming from the edge.
 *
 * @param readingUid      client-generated identifier used to de-duplicate retried deliveries
 * @param equipmentId     identifier of the equipment the reading belongs to
 * @param deviceId        identifier of the device that produced the reading
 * @param minTemperature  minimum temperature observed in the window
 * @param maxTemperature  maximum temperature observed in the window
 * @param avgTemperature  average temperature observed in the window
 * @param humidity        average humidity observed in the window, may be {@code null}
 * @param sampleCount     number of raw samples folded into this reading
 * @param recordedAt      timestamp assigned by the device clock
 */
public record RecordReadingBatchCommand(
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
