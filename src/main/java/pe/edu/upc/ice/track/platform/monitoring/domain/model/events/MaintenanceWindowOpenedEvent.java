package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain event registered when a maintenance window has been opened and persisted.
 *
 * @param maintenanceWindowId identity of the window
 * @param startEventUid       identifier of the gateway event that started it
 * @param equipmentId         the unit under maintenance
 * @param origin              how the window was started
 * @param startedAt           when the gateway started it, by its clock
 * @param expiresAt           when it expires unless closed earlier
 * @param technicianProfileId the technician working on the unit, or {@code null} when unknown
 * @param serviceRequestId    the order the window belongs to, or {@code null} when unlinked
 */
public record MaintenanceWindowOpenedEvent(
    Long maintenanceWindowId,
    UUID startEventUid,
    Long equipmentId,
    MaintenanceOrigin origin,
    Instant startedAt,
    Instant expiresAt,
    Long technicianProfileId,
    Long serviceRequestId) {

  /**
   * @param window the persisted window
   * @return the populated event
   */
  public static MaintenanceWindowOpenedEvent from(MaintenanceWindow window) {
    return new MaintenanceWindowOpenedEvent(
        window.getId(), window.getStartEventUid(), window.getEquipmentId(), window.getOrigin(),
        window.getStartedAt(), window.getExpiresAt(), window.getTechnicianProfileId(), window.getServiceRequestId());
  }
}
