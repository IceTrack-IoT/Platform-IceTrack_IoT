package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command moving a unit to {@code MAINTENANCE} because a maintenance window opened on it.
 *
 * <p>Issued by the inbound adapter reacting to Monitoring and Alerting's events, never by a REST
 * caller, so it carries no username and is not owner-scoped.</p>
 *
 * @param equipmentId         the unit; required
 * @param maintenanceWindowId the window that opened, for the logs; required
 */
public record EnterMaintenanceCommand(Long equipmentId, Long maintenanceWindowId) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when the unit or the window is missing
   */
  public EnterMaintenanceCommand {
    if (equipmentId == null || maintenanceWindowId == null) {
      throw new IllegalArgumentException("equipmentId and maintenanceWindowId must not be null");
    }
  }
}
