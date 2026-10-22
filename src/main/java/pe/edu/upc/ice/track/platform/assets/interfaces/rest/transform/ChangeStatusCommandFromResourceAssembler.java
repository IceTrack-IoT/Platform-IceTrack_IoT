package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeStatusResource;

/**
 * Assembler that converts a {@link ChangeStatusResource} into a {@link ChangeStatusCommand}.
 */
public class ChangeStatusCommandFromResourceAssembler {

  /**
   * Converts the status change payload into its command representation.
   *
   * @param equipmentId identifier of the unit, taken from the request path
   * @param username username of the authenticated caller
   * @param resource the {@link ChangeStatusResource} payload
   * @return the {@link ChangeStatusCommand} command
   */
  public static ChangeStatusCommand toCommandFromResource(
      Long equipmentId, String username, ChangeStatusResource resource) {
    return new ChangeStatusCommand(equipmentId, username, resource.newStatus().toDomain());
  }
}
