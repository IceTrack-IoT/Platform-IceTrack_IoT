package pe.edu.upc.ice.track.platform.service.request.application.commandservices;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.SubmitReviewCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.UpdateReviewCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service handling write operations on {@link Review}.
 */
public interface ReviewCommandService {

  /**
   * Review one of the caller's completed requests.
   *
   * @param command The {@link SubmitReviewCommand} Command
   * @return A {@link Result} containing the submitted {@link Review}, or an {@link ApplicationError}
   *         when the request is not the caller's, is not completed, was already reviewed, or a
   *         score is invalid
   */
  Result<Review, ApplicationError> handle(SubmitReviewCommand command);

  /**
   * Edit one of the caller's reviews within its edit window.
   *
   * @param command The {@link UpdateReviewCommand} Command
   * @return A {@link Result} containing the updated {@link Review}, or an {@link ApplicationError}
   *         when the review is not the caller's, its edit window has closed, or a score is invalid
   */
  Result<Review, ApplicationError> handle(UpdateReviewCommand command);
}
