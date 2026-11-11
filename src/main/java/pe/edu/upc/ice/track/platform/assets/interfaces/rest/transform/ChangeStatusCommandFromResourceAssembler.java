package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ChangeStatusCommand;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ChangeStatusResource;
import pe.edu.upc.ice.track.platform.shared.interfaces.rest.transform.IfMatchHeaderAssembler;

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
   * @param ifMatch the raw {@code If-Match} header, or {@code null}
   * @return the {@link ChangeStatusCommand} command
   */
  public static ChangeStatusCommand toCommandFromResource(
      Long equipmentId, String username, ChangeStatusResource resource, String ifMatch) {
    return new ChangeStatusCommand(
        equipmentId, username, resource.newStatus().toDomain(), IfMatchHeaderAssembler.toExpectedVersion(ifMatch));
  }
}
