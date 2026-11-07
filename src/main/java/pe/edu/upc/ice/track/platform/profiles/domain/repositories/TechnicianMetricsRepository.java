package pe.edu.upc.ice.track.platform.profiles.domain.repositories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.ProjectedReviewScore;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;

import java.util.Optional;

/**
 * Repository port of the technician metrics read model, together with the bookkeeping that makes its
 * projection idempotent.
 */
public interface TechnicianMetricsRepository {

  /**
   * @param technicianProfileId the technician
   * @return the technician's metrics, or empty when nothing was projected for it yet
   */
  Optional<TechnicianMetrics> findByTechnicianProfileId(Long technicianProfileId);

  /**
   * Persists a technician's metrics, version-checked.
   *
   * @param metrics the metrics
   * @return the persisted metrics
   */
  TechnicianMetrics save(TechnicianMetrics metrics);

  /**
   * Records that a completed service was counted.
   *
   * @param serviceRequestId    the completed request
   * @param technicianProfileId the technician who completed it
   * @return {@code true} when it had not been counted yet, {@code false} when it already was
   */
  boolean registerCompletedService(Long serviceRequestId, Long technicianProfileId);

  /**
   * @param reviewId the review
   * @return the score last counted for the review, or empty when it was never counted
   */
  Optional<ProjectedReviewScore> findReviewScore(Long reviewId);

  /**
   * Stores, or replaces, the score counted for a review.
   *
   * @param score the review's score
   */
  void saveReviewScore(ProjectedReviewScore score);
}
