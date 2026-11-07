package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;

/**
 * JPA persistence entity for a {@code MaintenanceWindowLink}, mapped to the
 * {@code service_request_maintenance_window_links} table.
 *
 * <p>Owned by its {@link ServiceRequestPersistenceEntity}: every row is written through the request's
 * {@code maintenanceWindows} collection, which cascades every operation. The unique constraint on
 * {@code (service_request_id, window_id)} backs the rule that a window is linked to a request once,
 * also when two deliveries of the same event race. {@code window_id} is a plain column: the window
 * lives in Monitoring and Alerting.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "service_request_maintenance_window_links",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_service_request_maintenance_window_links_request_window",
        columnNames = {"service_request_id", "window_id"}))
public class MaintenanceWindowLinkPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "service_request_id", nullable = false, updatable = false)
  private ServiceRequestPersistenceEntity serviceRequest;

  @Column(name = "window_id", nullable = false, updatable = false)
  private Long windowId;

  @Enumerated(EnumType.STRING)
  @Column(name = "origin", nullable = false, length = 10, updatable = false)
  private MaintenanceOrigin origin;

  @Column(name = "technician_id", updatable = false)
  private Long technicianId;

  @Column(name = "started_at", nullable = false, updatable = false)
  private Instant startedAt;

  @Column(name = "ended_at")
  private Instant endedAt;

  @Column(name = "closure_reason", length = 30)
  private String closureReason;

  public MaintenanceWindowLinkPersistenceEntity() {
  }
}
