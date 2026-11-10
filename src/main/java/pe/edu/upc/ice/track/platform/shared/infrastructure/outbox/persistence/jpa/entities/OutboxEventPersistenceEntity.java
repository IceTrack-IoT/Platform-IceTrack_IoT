package pe.edu.upc.ice.track.platform.shared.infrastructure.outbox.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

/**
 * JPA persistence entity for an integration event waiting in, or delivered from, the transactional
 * outbox, mapped to the {@code outbox_events} table.
 *
 * <p>{@code event_type} holds the fully qualified name of the integration event record and
 * {@code payload_json} its JSON form, so the dispatcher can rebuild the very event that was appended.
 * {@code published_at} stays {@code null} until a delivery succeeded; {@code attempts} and
 * {@code last_error} trace failed deliveries. The {@code (published_at, id)} index serves the
 * dispatcher's scan of pending events in insertion order.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "outbox_events",
    indexes = @Index(name = "idx_outbox_events_published_at_id", columnList = "published_at, id"))
public class OutboxEventPersistenceEntity {

  /** Longest failure message kept, in characters. */
  public static final int MAX_ERROR_LENGTH = 1000;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "aggregate_type", nullable = false, length = 60, updatable = false)
  private String aggregateType;

  @Column(name = "aggregate_id", nullable = false, updatable = false)
  private Long aggregateId;

  @Column(name = "event_type", nullable = false, length = 255, updatable = false)
  private String eventType;

  @Column(name = "payload_json", nullable = false, columnDefinition = "text", updatable = false)
  private String payloadJson;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "published_at")
  private Instant publishedAt;

  @ColumnDefault("0")
  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "last_error", length = MAX_ERROR_LENGTH)
  private String lastError;

  public OutboxEventPersistenceEntity() {
  }
}
