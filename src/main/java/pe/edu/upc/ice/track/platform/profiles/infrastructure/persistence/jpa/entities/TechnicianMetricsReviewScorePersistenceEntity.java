package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA persistence entity keeping the score each review contributes to the technician metrics read
 * model, mapped to the {@code technician_metrics_review_scores} table.
 *
 * <p>Keyed by the review identifier: an edit replaces the row's score instead of adding one, and
 * {@code scored_at} lets an older event delivered late be told apart from a newer one.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "technician_metrics_review_scores")
public class TechnicianMetricsReviewScorePersistenceEntity {

  @Id
  @Column(name = "review_id", nullable = false, updatable = false)
  private Long reviewId;

  @Column(name = "technician_profile_id", nullable = false)
  private Long technicianProfileId;

  @Column(name = "score", nullable = false)
  private double score;

  @Column(name = "scored_at", nullable = false)
  private Instant scoredAt;

  public TechnicianMetricsReviewScorePersistenceEntity() {
  }
}
