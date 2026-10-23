package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServicePriority;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceStatus;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.ServiceType;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * JPA persistence entity for a {@code ServiceRequest}, mapped to the {@code service_requests}
 * table.
 *
 * <p>Kept separate from the aggregate, exactly like the persistence entities of {@code assets}:
 * the aggregate stays free of JPA, while this class absorbs the mapping, the column widths and the
 * indexes.</p>
 *
 * <p>{@code ownerId}, {@code equipmentId}, {@code siteId} and {@code technicianId} are plain
 * columns, never foreign keys: owners and technicians live in {@code profiles}, units and sites in
 * {@code assets}, and this context must not reference their entities.</p>
 *
 * <p>{@code requestedAt} holds the creation instant of the aggregate. It is kept apart from the
 * inherited audit {@code created_at}, which is stamped a few milliseconds later by the auditing
 * listener, so that the request and the first entry of its history report the very same
 * instant.</p>
 *
 * <p>The two composite indexes serve the two listings this context answers - an owner's requests
 * and a technician's assignments, both newest first - and the {@code equipment_id} index serves the
 * intervention history of a unit.</p>
 *
 * <p>Both children cascade every operation and are only changed through this entity: the
 * {@code history} through {@link #addTransition}, the {@code intervention} through its setter. The
 * {@code version} has no setter at all, because JPA forbids an application from changing it.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "service_requests",
    indexes = {
        @Index(name = "idx_service_requests_owner_requested_at", columnList = "owner_id, requested_at"),
        @Index(name = "idx_service_requests_technician_requested_at", columnList = "technician_id, requested_at"),
        @Index(name = "idx_service_requests_equipment_id", columnList = "equipment_id")})
public class ServiceRequestPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "owner_id", nullable = false, updatable = false)
  private Long ownerId;

  @Column(name = "equipment_id", nullable = false, updatable = false)
  private Long equipmentId;

  @Column(name = "site_id", nullable = false, updatable = false)
  private Long siteId;

  /** The assigned technician; {@code null} while none is assigned or after a rejection. */
  @Column(name = "technician_id")
  private Long technicianId;

  /** Stored as {@code STRING}, never as an ordinal, for the same reason as the equipment enums. */
  @Enumerated(EnumType.STRING)
  @Column(name = "service_type", nullable = false, length = 30, updatable = false)
  private ServiceType type;

  @Enumerated(EnumType.STRING)
  @Column(name = "priority", nullable = false, length = 20, updatable = false)
  private ServicePriority priority;

  @Column(name = "description", nullable = false, length = 1000, updatable = false)
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private ServiceStatus status;

  /** The creation instant of the aggregate; see the class documentation. */
  @Column(name = "requested_at", nullable = false, updatable = false)
  private Instant requestedAt;

  @Column(name = "assigned_at")
  private Instant assignedAt;

  @Column(name = "started_at")
  private Instant startedAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Column(name = "canceled_at")
  private Instant canceledAt;

  @Column(name = "cancel_reason", length = 500)
  private String cancelReason;

  /**
   * Optimistic lock. A request has two independent writers - the owner and the assigned
   * technician - and without it a cancellation racing a start would let the slower one silently
   * overwrite the other.
   */
  @Version
  @Setter(AccessLevel.NONE)
  @Column(name = "version", nullable = false)
  private Long version;

  /**
   * The intervention that completed the request. This side owns the association, through the
   * unique {@code intervention_id} column, so that the intervention can be loaded lazily; the inverse
   * side of a one-to-one is always fetched eagerly.
   */
  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @JoinColumn(name = "intervention_id", unique = true)
  private InterventionPersistenceEntity intervention;

  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  @OneToMany(mappedBy = "serviceRequest", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  @OrderBy("id ASC")
  @BatchSize(size = 50)
  private List<StatusTransitionPersistenceEntity> history = new ArrayList<>();

  public ServiceRequestPersistenceEntity() {
  }

  /**
   * Returns the recorded transitions, in the order they were appended.
   *
   * @return a read-only view of the history
   */
  public List<StatusTransitionPersistenceEntity> getHistory() {
    return Collections.unmodifiableList(history);
  }

  /**
   * Appends a transition to this request; it is inserted when the request is saved.
   *
   * @param transition the transition to append
   */
  public void addTransition(StatusTransitionPersistenceEntity transition) {
    transition.setServiceRequest(this);
    history.add(transition);
  }
}
