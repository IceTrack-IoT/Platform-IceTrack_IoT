package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

/**
 * JPA persistence entity of the technician metrics read model, declared as the
 * {@code technician_metrics_projection} table (pluralized by the platform's naming strategy).
 *
 * <p>Keyed by the technician profile identifier itself: there is one row per technician, created by
 * the first event that concerns it. {@code average_rating} is stored for direct reads and stays
 * {@code NULL} until the first review. The {@code version} column rejects a write based on a stale
 * copy, so two instances projecting events of the same technician at once cannot lose an update; the
 * losing delivery is retried by the outbox.</p>
 */
@Getter
@Setter
@Entity
@Table(name = "technician_metrics_projection")
public class TechnicianMetricsProjectionPersistenceEntity {

  @Id
  @Column(name = "technician_profile_id", nullable = false, updatable = false)
  private Long technicianProfileId;

  @ColumnDefault("0")
  @Column(name = "completed_services_count", nullable = false)
  private int completedServicesCount;

  @ColumnDefault("0")
  @Column(name = "rating_sum", nullable = false)
  private double ratingSum;

  @ColumnDefault("0")
  @Column(name = "rating_count", nullable = false)
  private int ratingCount;

  @Column(name = "average_rating")
  private Double averageRating;

  @Version
  @Setter(AccessLevel.NONE)
  @Column(name = "version", nullable = false)
  private Long version;

  public TechnicianMetricsProjectionPersistenceEntity() {
  }
}
