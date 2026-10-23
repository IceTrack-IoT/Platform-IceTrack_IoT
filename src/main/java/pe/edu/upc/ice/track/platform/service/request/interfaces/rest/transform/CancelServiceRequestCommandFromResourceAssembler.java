package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CancelServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.CancelServiceRequestResource;

/**
 * Assembler that converts a {@link CancelServiceRequestResource} into a
 * {@link CancelServiceRequestCommand}.
 */
public class CancelServiceRequestCommandFromResourceAssembler {

  /**
   * Converts the cancellation payload into its command representation.
   *
   * @param serviceRequestId identifier of the request, taken from the request path
   * @param username         username of the authenticated caller
   * @param resource         the {@link CancelServiceRequestResource} payload
   * @return the {@link CancelServiceRequestCommand} command
   */
  public static CancelServiceRequestCommand toCommandFromResource(
      Long serviceRequestId, String username, CancelServiceRequestResource resource) {
    return new CancelServiceRequestCommand(serviceRequestId, username, resource.reason());
  }
}
