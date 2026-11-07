package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command to record one aggregated sensor reading batch coming from the edge.
 *
 * <p>{@code equipmentId} and {@code deviceId} always come from the authenticated device, never
 * from the request body.</p>
 *
 * @param readingUid           client-generated identifier used to de-duplicate retried deliveries
 * @param equipmentId          identifier of the equipment the authenticated device is paired with
 * @param deviceId             identifier of the authenticated device
 * @param minTemperature       calibrated minimum temperature observed in the window
 * @param maxTemperature       calibrated maximum temperature observed in the window
 * @param avgTemperature       calibrated average temperature observed in the window
 * @param rawMinTemperature    minimum temperature measured by the probe, before calibration
 * @param rawMaxTemperature    maximum temperature measured by the probe, before calibration
 * @param rawAvgTemperature    average temperature measured by the probe, before calibration
 * @param humidity             average relative humidity observed in the window
 * @param sampleCount          number of raw samples folded into this reading
 * @param lightLevelMax        highest light level inside the unit, 0 to 100 %, or {@code null}
 * @param doorOpenSeconds      seconds the door was detected open, or {@code null}
 * @param presenceDetected     whether presence was detected near the unit, or {@code null} when
 *                             unavailable
 * @param recordedAt           timestamp assigned by the device clock
 * @param appliedConfigVersion edge configuration version the gateway was running when it built
 *                             the batch
 */
public record RecordReadingBatchCommand(
    UUID readingUid,
    Long equipmentId,
    Long deviceId,
    Double minTemperature,
    Double maxTemperature,
    Double avgTemperature,
    Double rawMinTemperature,
    Double rawMaxTemperature,
    Double rawAvgTemperature,
    Double humidity,
    Integer sampleCount,
    Double lightLevelMax,
    Integer doorOpenSeconds,
    Boolean presenceDetected,
    LocalDateTime recordedAt,
    Long appliedConfigVersion) {
}
