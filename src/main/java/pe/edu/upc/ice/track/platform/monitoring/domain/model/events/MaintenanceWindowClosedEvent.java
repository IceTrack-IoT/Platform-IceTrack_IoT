package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;

/**
 * Domain event registered when a maintenance window is closed.
 *
 * @param maintenanceWindowId identity of the window
 * @param equipmentId         the unit that was under maintenance
 * @param origin              the origin the window ended with
 * @param startedAt           when the window started
 * @param endedAt             when it ended
 * @param closureReason       why it ended
 * @param technicianProfileId the technician attributed, or {@code null} when unknown
 * @param serviceRequestId    the order it was linked to, or {@code null} when unlinked
 */
public record MaintenanceWindowClosedEvent(
    Long maintenanceWindowId,
    Long equipmentId,
    MaintenanceOrigin origin,
    Instant startedAt,
    Instant endedAt,
    ClosureReason closureReason,
    Long technicianProfileId,
    Long serviceRequestId) {

  /**
   * @param window the window that was just closed
   * @return the populated event
   */
  public static MaintenanceWindowClosedEvent from(MaintenanceWindow window) {
    return new MaintenanceWindowClosedEvent(
        window.getId(), window.getEquipmentId(), window.getOrigin(), window.getStartedAt(), window.getEndedAt(),
        window.getClosureReason(), window.getTechnicianProfileId(), window.getServiceRequestId());
  }
}
