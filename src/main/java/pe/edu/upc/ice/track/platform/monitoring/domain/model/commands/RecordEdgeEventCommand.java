package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Command recording an event reported by an edge gateway.
 *
 * <p>The device and the unit come from the API key the gateway authenticated with, never from the
 * payload.</p>
 *
 * @param eventUid               identifier the gateway generated, used to de-duplicate retries; required
 * @param type                   the kind of event; required
 * @param deviceId               the authenticated device; required
 * @param equipmentId            the unit the device is paired with; required
 * @param occurredAt             when the gateway observed the event, by its clock; required
 * @param temperatureCelsius     the out-of-range temperature; required for a detection
 * @param peakTemperatureCelsius the most extreme temperature of the excursion, optional with a normalization
 * @param readingUid             the reading batch the detection belongs to, optional
 */
public record RecordEdgeEventCommand(
    UUID eventUid,
    EdgeEventType type,
    Long deviceId,
    Long equipmentId,
    LocalDateTime occurredAt,
    Double temperatureCelsius,
    Double peakTemperatureCelsius,
    UUID readingUid) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing, or a detection carries no
   *                                  temperature
   */
  public RecordEdgeEventCommand {
    if (eventUid == null || type == null || deviceId == null || equipmentId == null || occurredAt == null) {
      throw new IllegalArgumentException("event_uid, type, occurred_at and the authenticated device are required");
    }
    if (type == EdgeEventType.THERMAL_EXCURSION_DETECTED && temperatureCelsius == null) {
      throw new IllegalArgumentException("temperature_celsius is required for THERMAL_EXCURSION_DETECTED");
    }
  }
}
