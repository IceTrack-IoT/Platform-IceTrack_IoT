package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;

import java.time.Instant;

/**
 * Domain event published when an owner edits a review within its edit window.
 *
 * @param reviewId         the review
 * @param serviceRequestId the reviewed request
 * @param technicianId     the technician the review is about
 * @param averageRating    the mean of the three new scores
 * @param updatedAt        when the review was edited
 */
public record ReviewUpdatedEvent(
    Long reviewId,
    Long serviceRequestId,
    Long technicianId,
    Double averageRating,
    Instant updatedAt) {

  /**
   * Extracts the event fields from a review that has just been edited.
   *
   * @param review    the edited review
   * @param updatedAt when it was edited
   * @return the populated event
   */
  public static ReviewUpdatedEvent from(Review review, Instant updatedAt) {
    return new ReviewUpdatedEvent(
        review.getId().reviewId(),
        review.getServiceRequestId().serviceRequestId(),
        review.getTechnicianId().technicianProfileId(),
        review.getRating().average(),
        updatedAt);
  }
}
