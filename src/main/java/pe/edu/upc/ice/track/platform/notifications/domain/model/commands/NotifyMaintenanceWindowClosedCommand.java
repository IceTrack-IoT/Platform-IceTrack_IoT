package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import java.time.Instant;

/**
 * Command notifying the owner of a unit that a maintenance window on it closed.
 *
 * @param maintenanceWindowId the window; required
 * @param equipmentId         the unit; required
 * @param origin              the origin the window ended with, {@code APP} or {@code BUTTON}; required
 * @param endedAt             when it ended; required
 * @param closureReason       why it ended; required
 * @param technicianProfileId the technician attributed, or {@code null}
 * @param serviceRequestId    the order linked, or {@code null} when unlinked
 */
public record NotifyMaintenanceWindowClosedCommand(
    Long maintenanceWindowId,
    Long equipmentId,
    String origin,
    Instant endedAt,
    String closureReason,
    Long technicianProfileId,
    Long serviceRequestId) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public NotifyMaintenanceWindowClosedCommand {
    if (maintenanceWindowId == null || equipmentId == null || origin == null || endedAt == null
        || closureReason == null) {
      throw new IllegalArgumentException(
          "maintenanceWindowId, equipmentId, origin, endedAt and closureReason are required");
    }
  }
}
