package pe.edu.upc.ice.track.platform.assets.domain.model.commands;

/**
 * Command to increment the edge configuration version of a unit because one of the inputs of its
 * configuration changed outside this context: its alert policy, or the technician grants of its
 * service requests.
 *
 * <p>Issued by inbound adapters reacting to integration events, never by a REST caller, so it
 * carries no username and is not owner-scoped.</p>
 *
 * @param equipmentId the unit whose configuration changed; required
 * @param reason      what changed, for the logs; required
 */
public record BumpConfigVersionCommand(Long equipmentId, String reason) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when the unit or the reason is missing
   */
  public BumpConfigVersionCommand {
    if (equipmentId == null) {
      throw new IllegalArgumentException("equipmentId must not be null");
    }
    if (reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("reason must not be null or blank");
    }
  }
}
