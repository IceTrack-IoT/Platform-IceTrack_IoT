package pe.edu.upc.ice.track.platform.monitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventResult;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * JPA persistence entity for a processed {@code EdgeEvent}, mapped to the {@code edge_events} table.
 *
 * <p>The unique index on {@code event_uid} is what settles two concurrent deliveries of the same
 * event: the second insert is rejected and its transaction, alert change included, rolls back.
 * {@code created_at} is when the cloud received the event.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "edge_events",
    indexes = {
        @Index(name = "idx_edge_events_event_uid", columnList = "event_uid", unique = true),
        @Index(name = "idx_edge_events_equipment_id", columnList = "equipment_id")})
public class EdgeEventPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "event_uid", nullable = false)
  private UUID eventUid;

  @Column(name = "device_id", nullable = false)
  private Long deviceId;

  @Column(name = "equipment_id", nullable = false)
  private Long equipmentId;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 40)
  private EdgeEventType type;

  @Column(name = "occurred_at", nullable = false)
  private LocalDateTime occurredAt;

  @Column(name = "temperature_celsius")
  private Double temperatureCelsius;

  @Column(name = "peak_temperature_celsius")
  private Double peakTemperatureCelsius;

  @Enumerated(EnumType.STRING)
  @Column(name = "result", nullable = false, length = 30)
  private EdgeEventResult result;

  @Column(name = "alert_id")
  private Long alertId;

  public EdgeEventPersistenceEntity() {
  }
}
