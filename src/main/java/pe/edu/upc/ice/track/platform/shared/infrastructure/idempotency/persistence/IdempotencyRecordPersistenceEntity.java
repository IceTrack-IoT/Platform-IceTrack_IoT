package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity of the {@code idempotency_records} table: one row per {@code Idempotency-Key} a caller
 * sent to a creating endpoint, holding the outcome of the first request so a retry can be answered
 * with it.
 *
 * <p>The unique constraint on {@code (caller, endpoint, idempotency_key)} is what serializes two
 * requests carrying the same key: the second insert fails, so exactly one of them runs. A row is
 * {@code completed = false} while that first request is running, and keeps the stored answer once it
 * finished. Rows expire 24 hours after they were created and are purged afterwards.</p>
 */
@Entity
@Table(
    name = "idempotency_records",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_idempotency_records_caller_endpoint_key",
        columnNames = {"caller", "endpoint", "idempotency_key"}),
    indexes = @Index(name = "idx_idempotency_records_expires_at", columnList = "expires_at"))
@Getter
@Setter
@NoArgsConstructor
public class IdempotencyRecordPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Username of the authenticated caller that sent the key. */
  @Column(name = "caller", nullable = false, length = 100)
  private String caller;

  /** Method and path of the endpoint, such as {@code POST /api/v1/devices}. */
  @Column(name = "endpoint", nullable = false, length = 150)
  private String endpoint;

  /** The key the caller sent, a UUID in its canonical lower-case form. */
  @Column(name = "idempotency_key", nullable = false, length = 36)
  private String idempotencyKey;

  /** SHA-256 digest of the request body, hex encoded, to tell a retry from a different request. */
  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  /** Whether the first request finished and its answer is stored. */
  @Column(name = "completed", nullable = false)
  private boolean completed;

  /** HTTP status of the stored answer; {@code null} while the first request runs. */
  @Column(name = "response_status")
  private Integer responseStatus;

  /** Content type of the stored answer; {@code null} while the first request runs. */
  @Column(name = "response_content_type", length = 100)
  private String responseContentType;

  /** Body of the stored answer, with secrets already removed; {@code null} while the first request runs. */
  @Lob
  @Column(name = "response_body")
  private String responseBody;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** When the key stops being remembered; a retry after it runs as a new request. */
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;
}
