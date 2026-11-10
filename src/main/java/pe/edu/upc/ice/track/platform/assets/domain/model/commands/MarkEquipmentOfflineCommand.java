package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command recording that a unit stopped reporting, as detected by Monitoring and Alerting.
 *
 * <p>Issued through the published facade of this context, never by a REST caller, so it carries no
 * username and is not owner-scoped.</p>
 *
 * @param equipmentId the unit; required
 */
public record MarkEquipmentOfflineCommand(Long equipmentId) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when the unit is missing
   */
  public MarkEquipmentOfflineCommand {
    if (equipmentId == null) {
      throw new IllegalArgumentException("equipmentId must not be null");
    }
  }
}
