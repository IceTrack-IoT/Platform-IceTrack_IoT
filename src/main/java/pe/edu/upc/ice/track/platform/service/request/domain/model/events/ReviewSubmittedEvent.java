package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;

import java.time.Instant;

/**
 * Domain event published when an owner submits the review of a completed service request.
 *
 * @param reviewId         the identity assigned to the new review
 * @param serviceRequestId the reviewed request
 * @param ownerId          the owner who wrote the review
 * @param technicianId     the technician the review is about
 * @param averageRating    the mean of the three scores
 * @param submittedAt      when the review was submitted
 */
public record ReviewSubmittedEvent(
    Long reviewId,
    Long serviceRequestId,
    Long ownerId,
    Long technicianId,
    Double averageRating,
    Instant submittedAt) {

  /**
   * Extracts the event fields from a saved {@link Review}.
   *
   * @param review the saved review (must already carry its identity)
   * @return the populated event
   */
  public static ReviewSubmittedEvent from(Review review) {
    return new ReviewSubmittedEvent(
        review.getId().reviewId(),
        review.getServiceRequestId().serviceRequestId(),
        review.getOwnerId().ownerId(),
        review.getTechnicianId().technicianProfileId(),
        review.getRating().average(),
        review.getCreatedAt());
  }
}
