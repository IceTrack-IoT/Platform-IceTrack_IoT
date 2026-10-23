package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegisterInterventionCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.RegisterInterventionResource;

/**
 * Assembler that converts a {@link RegisterInterventionResource} into a
 * {@link RegisterInterventionCommand}.
 */
public class RegisterInterventionCommandFromResourceAssembler {

  /**
   * Converts the intervention payload into its command representation.
   *
   * @param username username of the authenticated caller
   * @param resource the {@link RegisterInterventionResource} payload
   * @return the {@link RegisterInterventionCommand} command
   */
  public static RegisterInterventionCommand toCommandFromResource(String username, RegisterInterventionResource resource) {
    return new RegisterInterventionCommand(
        resource.serviceRequestId(),
        username,
        resource.clientGeneratedId(),
        resource.diagnosis(),
        resource.actionsTaken(),
        resource.spareParts(),
        resource.photoEvidence(),
        resource.completedAt());
  }
}
