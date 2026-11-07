package pe.edu.upc.ice.track.platform.profiles.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.ProjectedReviewScore;
import pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels.TechnicianMetrics;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianMetricsRepository;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ReviewSubmittedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ReviewUpdatedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.interfaces.events.ServiceRequestCompletedIntegrationEvent;

import java.time.Instant;

/**
 * Projects Service Request Management's events into the technician metrics read model.
 *
 * <p>The events reach this handler through the transactional outbox, after the transaction that
 * completed the request or saved the review committed, so the projection never slows down or fails a
 * write in the originating context. It runs inside the outbox dispatcher's delivery transaction: when
 * it fails, its writes roll back and the event is delivered again later.</p>
 *
 * <p>Delivery is at least once, so every update is idempotent:</p>
 * <ul>
 *   <li>a completed service is counted once per {@code serviceRequestId};</li>
 *   <li>a review score is counted once per {@code reviewId}; an edit replaces the review's previous
 *       score, and an event older than the score already counted - a late redelivery - is ignored.</li>
 * </ul>
 * <p>{@code averageRating} is {@code rating_sum / rating_count}, and stays absent until the first
 * review.</p>
 */
@Service
@Slf4j
public class TechnicianMetricsProjector {

  private final TechnicianMetricsRepository technicianMetricsRepository;

  public TechnicianMetricsProjector(TechnicianMetricsRepository technicianMetricsRepository) {
    this.technicianMetricsRepository = technicianMetricsRepository;
  }

  /**
   * Counts a completed service for its technician.
   *
   * @param event the integration event delivered from the outbox
   */
  @EventListener
  @Transactional
  public void on(ServiceRequestCompletedIntegrationEvent event) {
    if (event.serviceRequestId() == null || event.technicianProfileId() == null) {
      log.warn("Ignored a service completion without request or technician: {}", event);
      return;
    }
    if (!technicianMetricsRepository.registerCompletedService(event.serviceRequestId(), event.technicianProfileId())) {
      log.debug("Completion of service request {} was already counted", event.serviceRequestId());
      return;
    }
    var metrics = metricsOf(event.technicianProfileId());
    metrics.recordCompletedService();
    technicianMetricsRepository.save(metrics);
  }

  /**
   * Counts the score of a new review. A review already counted - a redelivery, or an edit delivered
   * first - is left as it is.
   *
   * @param event the integration event delivered from the outbox
   */
  @EventListener
  @Transactional
  public void on(ReviewSubmittedIntegrationEvent event) {
    if (!isValid(event.reviewId(), event.technicianProfileId(), event.averageRating())) {
      log.warn("Ignored an incomplete review submission: {}", event);
      return;
    }
    if (technicianMetricsRepository.findReviewScore(event.reviewId()).isPresent()) {
      log.debug("Review {} was already counted", event.reviewId());
      return;
    }
    count(event.reviewId(), event.technicianProfileId(), event.averageRating(), event.submittedAt(), null);
  }

  /**
   * Replaces the score of an edited review, or counts it when its submission was not projected yet.
   *
   * @param event the integration event delivered from the outbox
   */
  @EventListener
  @Transactional
  public void on(ReviewUpdatedIntegrationEvent event) {
    if (!isValid(event.reviewId(), event.technicianProfileId(), event.averageRating())) {
      log.warn("Ignored an incomplete review edit: {}", event);
      return;
    }
    var previous = technicianMetricsRepository.findReviewScore(event.reviewId());
    if (previous.isPresent() && event.updatedAt() != null && previous.get().scoredAt() != null
        && !event.updatedAt().isAfter(previous.get().scoredAt())) {
      log.debug("Edit of review {} at {} is not newer than the score counted; ignored",
          event.reviewId(), event.updatedAt());
      return;
    }
    count(event.reviewId(), event.technicianProfileId(), event.averageRating(), event.updatedAt(),
        previous.orElse(null));
  }

  /**
   * Adds or replaces a review's score in its technician's metrics.
   *
   * @param previous the score counted so far, or {@code null} when the review was never counted
   */
  private void count(Long reviewId, Long technicianProfileId, double score, Instant scoredAt,
                     ProjectedReviewScore previous) {
    var metrics = metricsOf(technicianProfileId);
    if (previous == null) {
      metrics.addRating(score);
    } else {
      metrics.replaceRating(previous.score(), score);
    }
    technicianMetricsRepository.saveReviewScore(new ProjectedReviewScore(
        reviewId, technicianProfileId, score, scoredAt == null ? Instant.now() : scoredAt));
    technicianMetricsRepository.save(metrics);
  }

  private TechnicianMetrics metricsOf(Long technicianProfileId) {
    return technicianMetricsRepository.findByTechnicianProfileId(technicianProfileId)
        .orElseGet(() -> TechnicianMetrics.empty(technicianProfileId));
  }

  private static boolean isValid(Long reviewId, Long technicianProfileId, Double averageRating) {
    return reviewId != null && technicianProfileId != null && averageRating != null
        && Double.isFinite(averageRating);
  }
}
