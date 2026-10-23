package pe.edu.upc.ice.track.platform.service.request.domain.repositories;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ReviewId;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceRequestId;

import java.util.Optional;

/**
 * Review repository port.
 *
 * <p>Implemented by {@code ReviewRepositoryImpl} in the infrastructure layer, which also publishes
 * the {@code ReviewSubmittedEvent} of a new review once it has an identity.</p>
 */
public interface ReviewRepository {

  /**
   * Find a review by its identifier.
   *
   * @param reviewId the review identifier
   * @return the review, or empty when none carries that identifier
   */
  Optional<Review> findById(ReviewId reviewId);

  /**
   * Find the review of a service request.
   *
   * @param serviceRequestId the reviewed request
   * @return the review, or empty when the request has not been reviewed
   */
  Optional<Review> findByServiceRequestId(ServiceRequestId serviceRequestId);

  /**
   * Check whether a service request has already been reviewed.
   *
   * <p>Backs the one-review-per-request rule: the check runs before the insert so the caller gets a
   * conflict rather than a constraint violation, while the unique constraint on
   * {@code service_request_id} remains the real guarantee under concurrency.</p>
   *
   * @param serviceRequestId the request
   * @return {@code true} when a review exists for it
   */
  boolean existsByServiceRequestId(ServiceRequestId serviceRequestId);

  /**
   * Persist a review, inserting it when it is new and updating it otherwise.
   *
   * @param review the review to persist
   * @return the persisted review, carrying its identity
   */
  Review save(Review review);
}
