package pe.edu.upc.ice.track.platform.service.request.application.queryservices;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.Review;
import pe.edu.upc.ice.track.platform.service.request.domain.model.queries.GetReviewByServiceRequestIdQuery;

import java.util.Optional;

/**
 * Application service handling read operations on {@link Review}.
 */
public interface ReviewQueryService {

  /**
   * Fetch the review of a service request.
   *
   * @param query The {@link GetReviewByServiceRequestIdQuery} Query
   * @return the review, or empty when there is none or the caller is neither the owner who wrote it
   *         nor the technician it is about
   */
  Optional<Review> handle(GetReviewByServiceRequestIdQuery query);
}
