package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RejectServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.RejectServiceRequestResource;

/**
 * Assembler that converts a {@link RejectServiceRequestResource} into a
 * {@link RejectServiceRequestCommand}.
 */
public class RejectServiceRequestCommandFromResourceAssembler {

  /**
   * Converts the rejection payload into its command representation.
   *
   * @param serviceRequestId identifier of the request, taken from the request path
   * @param username         username of the authenticated caller
   * @param resource         the {@link RejectServiceRequestResource} payload
   * @return the {@link RejectServiceRequestCommand} command
   */
  public static RejectServiceRequestCommand toCommandFromResource(
      Long serviceRequestId, String username, RejectServiceRequestResource resource) {
    return new RejectServiceRequestCommand(serviceRequestId, username, resource.reason());
  }
}
