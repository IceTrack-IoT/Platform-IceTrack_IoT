package pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects;

/**
 * Identifier of a review.
 *
 * @param reviewId the persistence identity; required and strictly positive
 */
public record ReviewId(Long reviewId) {

  /**
   * Validates the identifier.
   *
   * @throws IllegalArgumentException when the identifier is missing or not positive
   */
  public ReviewId {
    if (reviewId == null || reviewId <= 0) {
      throw new IllegalArgumentException("Review identifier must be a positive number");
    }
  }
}
