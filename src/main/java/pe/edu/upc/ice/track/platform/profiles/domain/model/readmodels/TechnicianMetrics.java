package pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels;

import lombok.Getter;

import java.util.Objects;

/**
 * Read model of a technician's operational statistics, projected from Service Request Management's
 * events rather than computed by querying that context on every read.
 *
 * <p>It counts the services the technician completed and keeps the sum and the count of the review
 * scores received, from which {@code averageRating} is derived. A technician who has never been
 * reviewed has no average at all: {@code averageRating} is {@code null}, never {@code 0.0}, because a
 * zero would read as the worst possible rating.</p>
 *
 * <p>The idempotency of the projection - counting a completed service or a review once however many
 * times its event is delivered - is the projector's job; this class only applies the arithmetic.</p>
 */
@Getter
public class TechnicianMetrics {

  private final Long technicianProfileId;
  private int completedServicesCount;
  private double ratingSum;
  private int ratingCount;
  private Double averageRating;
  private final Long version;

  /**
   * Reconstitutes the metrics of a technician.
   *
   * @param technicianProfileId    the technician; required
   * @param completedServicesCount services completed so far; not negative
   * @param ratingSum              sum of the review scores received
   * @param ratingCount            number of review scores received; not negative
   * @param version                optimistic concurrency token, or {@code null} for metrics not yet stored
   */
  public TechnicianMetrics(Long technicianProfileId, int completedServicesCount, double ratingSum, int ratingCount,
                           Long version) {
    this.technicianProfileId = Objects.requireNonNull(technicianProfileId, "technicianProfileId must not be null");
    if (completedServicesCount < 0 || ratingCount < 0) {
      throw new IllegalArgumentException("Technician metrics counters must not be negative");
    }
    this.completedServicesCount = completedServicesCount;
    this.ratingSum = ratingSum;
    this.ratingCount = ratingCount;
    this.version = version;
    recomputeAverage();
  }

  /**
   * @param technicianProfileId the technician; required
   * @return metrics with no completed service and no review
   */
  public static TechnicianMetrics empty(Long technicianProfileId) {
    return new TechnicianMetrics(technicianProfileId, 0, 0.0, 0, null);
  }

  /** Counts one more completed service. */
  public void recordCompletedService() {
    completedServicesCount++;
  }

  /**
   * Adds the score of a review received for the first time.
   *
   * @param score the review's average score
   */
  public void addRating(double score) {
    ratingSum += score;
    ratingCount++;
    recomputeAverage();
  }

  /**
   * Replaces the score of a review already counted, after the owner edited it.
   *
   * @param previousScore the score counted so far
   * @param newScore      the edited score
   */
  public void replaceRating(double previousScore, double newScore) {
    if (ratingCount == 0) {
      addRating(newScore);
      return;
    }
    ratingSum += newScore - previousScore;
    recomputeAverage();
  }

  /**
   * @return {@code true} once at least one review was received
   */
  public boolean isRatingAvailable() {
    return averageRating != null;
  }

  private void recomputeAverage() {
    this.averageRating = ratingCount == 0 ? null : ratingSum / ratingCount;
  }
}
