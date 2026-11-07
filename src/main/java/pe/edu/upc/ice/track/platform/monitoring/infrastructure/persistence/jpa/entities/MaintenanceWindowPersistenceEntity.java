package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA persistence entity for a {@code MaintenanceWindow}, mapped to the
 * {@code monitoring_maintenance_windows} table.
 *
 * <p>The unique index on {@code start_event_uid} settles two concurrent deliveries of the same start
 * event: the second insert is rejected and its transaction rolls back. The
 * {@code (equipment_id, started_at)} index serves the coverage lookup run for every reading and
 * gateway event, and the per-unit listing.</p>
 *
 * <p>{@code technician_profile_id} and {@code service_request_id} are plain columns: technicians live
 * in {@code profiles} and orders in {@code service.request}, and this context never references their
 * entities.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "monitoring_maintenance_windows",
    indexes = {
        @Index(name = "idx_maintenance_windows_start_event_uid", columnList = "start_event_uid", unique = true),
        @Index(name = "idx_maintenance_windows_equipment_started_at", columnList = "equipment_id, started_at")})
public class MaintenanceWindowPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "start_event_uid", nullable = false, updatable = false)
  private UUID startEventUid;

  @Column(name = "equipment_id", nullable = false, updatable = false)
  private Long equipmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false, length = 10)
  private MaintenanceOrigin origin;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "closure_reason", length = 30)
  private ClosureReason closureReason;

  @Column(name = "technician_profile_id")
  private Long technicianProfileId;

  @Column(name = "service_request_id")
  private Long serviceRequestId;

  public MaintenanceWindowPersistenceEntity() {
  }
}
