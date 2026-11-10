package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.UpdateReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources.UpdateReviewResource;

/**
 * Assembler that converts an {@link UpdateReviewResource} into an {@link UpdateReviewCommand}.
 */
public class UpdateReviewCommandFromResourceAssembler {

  /**
   * Converts the review edit payload into its command representation.
   *
   * @param reviewId identifier of the review, taken from the request path
   * @param username username of the authenticated caller
   * @param resource the {@link UpdateReviewResource} payload
   * @return the {@link UpdateReviewCommand} command
   */
  public static UpdateReviewCommand toCommandFromResource(Long reviewId, String username, UpdateReviewResource resource) {
    return new UpdateReviewCommand(
        reviewId,
        username,
        resource.communication(),
        resource.efficacy(),
        resource.performance(),
        resource.comment());
  }
}
