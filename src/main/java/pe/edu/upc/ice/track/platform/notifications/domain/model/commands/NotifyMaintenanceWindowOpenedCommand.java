package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import java.time.Instant;

/**
 * Command notifying the owner of a unit that a maintenance window opened on it.
 *
 * <p>A window with no service request is unlinked: nobody is accountable for the unit being in
 * maintenance, so the owner is warned about it at once.</p>
 *
 * @param maintenanceWindowId the window; required
 * @param equipmentId         the unit; required
 * @param origin              how the window was started, {@code APP} or {@code BUTTON}; required
 * @param startedAt           when it started; required
 * @param expiresAt           when it expires unless closed earlier; required
 * @param technicianProfileId the technician attributed, or {@code null}
 * @param serviceRequestId    the order linked, or {@code null} when unlinked
 */
public record NotifyMaintenanceWindowOpenedCommand(
    Long maintenanceWindowId,
    Long equipmentId,
    String origin,
    Instant startedAt,
    Instant expiresAt,
    Long technicianProfileId,
    Long serviceRequestId) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public NotifyMaintenanceWindowOpenedCommand {
    if (maintenanceWindowId == null || equipmentId == null || origin == null || startedAt == null
        || expiresAt == null) {
      throw new IllegalArgumentException("maintenanceWindowId, equipmentId, origin, startedAt and expiresAt are required");
    }
  }

  /**
   * @return {@code true} when no service request is linked to the window
   */
  public boolean unlinked() {
    return serviceRequestId == null;
  }
}
