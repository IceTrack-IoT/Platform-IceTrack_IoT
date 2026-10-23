package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CreateServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.CreateServiceRequestResource;

/**
 * Assembler that converts a {@link CreateServiceRequestResource} into a
 * {@link CreateServiceRequestCommand}.
 */
public class CreateServiceRequestCommandFromResourceAssembler {

  /**
   * Converts the creation payload into its command representation.
   *
   * @param username username of the authenticated caller
   * @param resource the {@link CreateServiceRequestResource} payload
   * @return the {@link CreateServiceRequestCommand} command
   */
  public static CreateServiceRequestCommand toCommandFromResource(String username, CreateServiceRequestResource resource) {
    return new CreateServiceRequestCommand(
        username,
        resource.equipmentId(),
        resource.type().toDomain(),
        resource.priority().toDomain(),
        resource.description());
  }
}
