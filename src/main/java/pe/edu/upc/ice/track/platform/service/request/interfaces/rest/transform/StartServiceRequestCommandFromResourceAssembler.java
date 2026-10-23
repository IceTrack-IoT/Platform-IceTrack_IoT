package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.StartServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.StartServiceRequestResource;

/**
 * Assembler that converts an optional {@link StartServiceRequestResource} into a
 * {@link StartServiceRequestCommand}.
 */
public class StartServiceRequestCommandFromResourceAssembler {

  /**
   * Converts the start payload into its command representation.
   *
   * @param serviceRequestId identifier of the request, taken from the request path
   * @param username         username of the authenticated caller
   * @param resource         the {@link StartServiceRequestResource} payload, {@code null} when the
   *                         request carried no body
   * @return the {@link StartServiceRequestCommand} command
   */
  public static StartServiceRequestCommand toCommandFromResource(
      Long serviceRequestId, String username, StartServiceRequestResource resource) {
    return new StartServiceRequestCommand(serviceRequestId, username, resource == null ? null : resource.startedAt());
  }
}
