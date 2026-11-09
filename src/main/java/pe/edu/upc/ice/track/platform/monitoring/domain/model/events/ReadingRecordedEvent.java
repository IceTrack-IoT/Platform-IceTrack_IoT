package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event registered when a newly received reading batch is recorded.
 *
 * <p>Registered by {@code SensorReading.record} only, never when a stored reading is reconstituted,
 * and published by the repository adapter once the batch is saved. It identifies the batch by its
 * client-generated {@code readingUid}, which is unique across the platform, because the surrogate
 * identifier is not assigned yet when the event is registered.</p>
 *
 * <p>Consumed, after the ingestion transaction commits, by the telemetry stream, which pushes the
 * batch to the live dashboards of the unit's owner. A batch whose transaction rolls back, such as a
 * concurrent duplicate delivery, is therefore never streamed.</p>
 *
 * @param readingUid       client-generated identifier of the batch
 * @param equipmentId      identifier of the unit the batch was taken on
 * @param deviceId         identifier of the device that produced the batch
 * @param minTemperature   calibrated minimum temperature, in Celsius
 * @param maxTemperature   calibrated maximum temperature, in Celsius
 * @param avgTemperature   calibrated average temperature, in Celsius
 * @param humidity         average relative humidity, as a percentage
 * @param sampleCount      number of raw samples folded into the batch
 * @param recordedAt       when the batch was taken, by the device clock
 * @param receivedAt       when the platform received the batch
 * @param underMaintenance whether a maintenance window covered {@code recordedAt}
 */
public record ReadingRecordedEvent(
    UUID readingUid,
    Long equipmentId,
    Long deviceId,
    Double minTemperature,
    Double maxTemperature,
    Double avgTemperature,
    Double humidity,
    Integer sampleCount,
    Instant recordedAt,
    Instant receivedAt,
    boolean underMaintenance) {
}
