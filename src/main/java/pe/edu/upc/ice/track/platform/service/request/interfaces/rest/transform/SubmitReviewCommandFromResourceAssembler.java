package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.SubmitReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.SubmitReviewResource;

/**
 * Assembler that converts a {@link SubmitReviewResource} into a {@link SubmitReviewCommand}.
 */
public class SubmitReviewCommandFromResourceAssembler {

  /**
   * Converts the review payload into its command representation.
   *
   * @param username username of the authenticated caller
   * @param resource the {@link SubmitReviewResource} payload
   * @return the {@link SubmitReviewCommand} command
   */
  public static SubmitReviewCommand toCommandFromResource(String username, SubmitReviewResource resource) {
    return new SubmitReviewCommand(
        resource.serviceRequestId(),
        username,
        resource.communication(),
        resource.efficacy(),
        resource.performance(),
        resource.comment());
  }
}
