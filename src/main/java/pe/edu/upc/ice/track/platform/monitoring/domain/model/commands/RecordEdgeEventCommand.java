package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;
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
 * @param origin                 how a maintenance window was started; required for a window start
 * @param technicianProfileId    the technician a maintenance window belongs to, optional
 * @param serviceRequestId       the order a maintenance window belongs to, optional
 * @param closureReason          why a maintenance window ended; required for a window end
 * @param startEventUid          the {@code event_uid} that started the window being ended, optional:
 *                               without it the unit's active window is ended
 */
public record RecordEdgeEventCommand(
    UUID eventUid,
    EdgeEventType type,
    Long deviceId,
    Long equipmentId,
    Instant occurredAt,
    Double temperatureCelsius,
    Double peakTemperatureCelsius,
    UUID readingUid,
    MaintenanceOrigin origin,
    Long technicianProfileId,
    Long serviceRequestId,
    ClosureReason closureReason,
    UUID startEventUid) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing, a detection carries no
   *                                  temperature, a window start no origin or a window end no
   *                                  closure reason
   */
  public RecordEdgeEventCommand {
    if (eventUid == null || type == null || deviceId == null || equipmentId == null || occurredAt == null) {
      throw new IllegalArgumentException("event_uid, type, occurred_at and the authenticated device are required");
    }
    if (type == EdgeEventType.THERMAL_EXCURSION_DETECTED && temperatureCelsius == null) {
      throw new IllegalArgumentException("temperature_celsius is required for THERMAL_EXCURSION_DETECTED");
    }
    if (type == EdgeEventType.MAINTENANCE_WINDOW_STARTED && origin == null) {
      throw new IllegalArgumentException("origin is required for MAINTENANCE_WINDOW_STARTED");
    }
    if (type == EdgeEventType.MAINTENANCE_WINDOW_ENDED && closureReason == null) {
      throw new IllegalArgumentException("closure_reason is required for MAINTENANCE_WINDOW_ENDED");
    }
    if ((technicianProfileId != null && technicianProfileId <= 0)
        || (serviceRequestId != null && serviceRequestId <= 0)) {
      throw new IllegalArgumentException("technician_id and service_request_id must be positive when sent");
    }
  }
}
