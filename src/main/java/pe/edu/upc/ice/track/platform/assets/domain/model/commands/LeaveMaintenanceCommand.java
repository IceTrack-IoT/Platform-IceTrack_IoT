package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command returning a unit to the status it held before a maintenance window moved it to
 * {@code MAINTENANCE}, because that window closed.
 *
 * <p>Issued by the inbound adapter reacting to Monitoring and Alerting's events, never by a REST
 * caller, so it carries no username and is not owner-scoped.</p>
 *
 * @param equipmentId         the unit; required
 * @param maintenanceWindowId the window that closed, for the logs; required
 */
public record LeaveMaintenanceCommand(Long equipmentId, Long maintenanceWindowId) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when the unit or the window is missing
   */
  public LeaveMaintenanceCommand {
    if (equipmentId == null || maintenanceWindowId == null) {
      throw new IllegalArgumentException("equipmentId and maintenanceWindowId must not be null");
    }
  }
}
