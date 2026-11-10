package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AssignTechnicianCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.AssignTechnicianResource;

/**
 * Assembler that converts an {@link AssignTechnicianResource} into an
 * {@link AssignTechnicianCommand}.
 */
public class AssignTechnicianCommandFromResourceAssembler {

  /**
   * Converts the assignment payload into its command representation.
   *
   * @param serviceRequestId identifier of the request, taken from the request path
   * @param username         username of the authenticated caller
   * @param resource         the {@link AssignTechnicianResource} payload
   * @return the {@link AssignTechnicianCommand} command
   */
  public static AssignTechnicianCommand toCommandFromResource(
      Long serviceRequestId, String username, AssignTechnicianResource resource) {
    return new AssignTechnicianCommand(serviceRequestId, resource.technicianProfileId(), username);
  }
}
