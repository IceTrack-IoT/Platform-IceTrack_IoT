package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * JPA persistence entity for an {@code Intervention}, mapped to the
 * {@code service_request_interventions} table.
 *
 * <p>Owned by the {@link ServiceRequestPersistenceEntity} that references it, which cascades every
 * operation. An intervention is immutable once registered, so every column is
 * {@code updatable = false}.</p>
 *
 * <p>{@code client_generated_id} carries a unique index. It is the final authority on the
 * idempotency of a submission: two concurrent submissions with the same identifier both pass the
 * application check, and the index rejects the second insert, which {@code GlobalExceptionHandler}
 * answers with {@code 409 DUPLICATE_RESOURCE_CONFLICT}; retrying then finds the first one and is
 * answered as a replay.</p>
 *
 * <p>The spare parts and the photo references are element collections with an order column, so the
 * order the technician entered them in survives a round trip. The entity and both collections are
 * batch-fetched, so that a page of requests does not load its interventions one row at a time.</p>
 */
@Getter
@Setter
@Entity
@BatchSize(size = 50)
@Table(
    name = "service_request_interventions",
    indexes = {
        @Index(
            name = "idx_service_request_interventions_client_generated_id",
            columnList = "client_generated_id",
            unique = true),
        @Index(name = "idx_service_request_interventions_equipment_id", columnList = "equipment_id")})
public class InterventionPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "client_generated_id", nullable = false, updatable = false)
  private UUID clientGeneratedId;

  @Column(name = "technician_id", nullable = false, updatable = false)
  private Long technicianId;

  @Column(name = "equipment_id", nullable = false, updatable = false)
  private Long equipmentId;

  @Column(name = "diagnosis", nullable = false, length = 2000, updatable = false)
  private String diagnosis;

  @Column(name = "actions_taken", nullable = false, length = 2000, updatable = false)
  private String actionsTaken;

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "intervention_spare_parts", joinColumns = @JoinColumn(name = "intervention_id"))
  @OrderColumn(name = "item_order")
  @Column(name = "spare_part", nullable = false, length = 100)
  @BatchSize(size = 50)
  private List<String> spareParts = new ArrayList<>();

  @ElementCollection(fetch = FetchType.LAZY)
  @CollectionTable(name = "intervention_photos", joinColumns = @JoinColumn(name = "intervention_id"))
  @OrderColumn(name = "item_order")
  @Column(name = "photo_reference", nullable = false, length = 2048)
  @BatchSize(size = 50)
  private List<String> photoEvidence = new ArrayList<>();

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "completed_at", nullable = false, updatable = false)
  private Instant completedAt;

  public InterventionPersistenceEntity() {
  }
}
