package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;

/**
 * JPA persistence entity for a {@code StatusTransition}, mapped to the
 * {@code service_request_status_transitions} table.
 *
 * <p>Owned by its {@link ServiceRequestPersistenceEntity}: every row is inserted through the
 * request's {@code history} collection, which cascades every operation. History is append-only, so
 * every column is {@code updatable = false} and there is deliberately no Spring Data repository for
 * this entity.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "service_request_status_transitions",
    indexes = @Index(
        name = "idx_service_request_status_transitions_request_id",
        columnList = "service_request_id"))
public class StatusTransitionPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_request_id", nullable = false, updatable = false)
  private ServiceRequestPersistenceEntity serviceRequest;

  /** The status before the transition; {@code null} only for the creation of the request. */
  @Enumerated(EnumType.STRING)
  @Column(name = "from_status", length = 20, updatable = false)
  private ServiceStatus fromStatus;

  @Enumerated(EnumType.STRING)
  @Column(name = "to_status", nullable = false, length = 20, updatable = false)
  private ServiceStatus toStatus;

  /** Profile identifier of the owner or technician who acted. */
  @Column(name = "actor_id", nullable = false, updatable = false)
  private Long actorId;

  @Column(name = "occurred_at", nullable = false, updatable = false)
  private Instant occurredAt;

  @Column(name = "reason", length = 500, updatable = false)
  private String reason;

  /** The technician the transition concerns, which after a rejection is the one who rejected. */
  @Column(name = "technician_id", updatable = false)
  private Long technicianId;

  public StatusTransitionPersistenceEntity() {
  }
}
