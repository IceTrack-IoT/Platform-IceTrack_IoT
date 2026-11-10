package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Check;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;

/**
 * JPA persistence entity for a {@code Review}, mapped to the {@code service_request_reviews} table.
 *
 * <p>The unique constraint on {@code service_request_id} is what guarantees one review per request
 * under concurrency: the application check cannot see a review submitted by a concurrent request,
 * and the constraint rejects the second insert, which {@code GlobalExceptionHandler} answers with
 * {@code 409 DUPLICATE_RESOURCE_CONFLICT}.</p>
 *
 * <p>The 1-to-5 range of each score is enforced by {@code ReviewRating} and re-asserted by a
 * {@code CHECK} constraint, as the database is the last line of defense against a value that
 * bypassed the domain. {@code submittedAt} holds the creation instant of the aggregate, kept apart
 * from the inherited audit {@code created_at} so that {@code editableUntil} is exactly
 * {@code submittedAt} plus the edit window.</p>
 *
 * <p>The index on {@code technician_id} serves reading the reviews about a technician.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "service_request_reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_service_request_reviews_service_request_id",
        columnNames = "service_request_id"),
    indexes = @Index(name = "idx_service_request_reviews_technician_id", columnList = "technician_id"))
@Check(constraints = "communication BETWEEN 1 AND 5 AND efficacy BETWEEN 1 AND 5 AND performance BETWEEN 1 AND 5")
public class ReviewPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "service_request_id", nullable = false, updatable = false)
  private Long serviceRequestId;

  @Column(name = "owner_id", nullable = false, updatable = false)
  private Long ownerId;

  @Column(name = "technician_id", nullable = false, updatable = false)
  private Long technicianId;

  @Column(name = "communication", nullable = false)
  private Integer communication;

  @Column(name = "efficacy", nullable = false)
  private Integer efficacy;

  @Column(name = "performance", nullable = false)
  private Integer performance;

  @Column(name = "comment", length = 1000)
  private String comment;

  /** The creation instant of the aggregate; see the class documentation. */
  @Column(name = "submitted_at", nullable = false, updatable = false)
  private Instant submittedAt;

  @Column(name = "editable_until", nullable = false, updatable = false)
  private Instant editableUntil;

  public ReviewPersistenceEntity() {
  }
}
