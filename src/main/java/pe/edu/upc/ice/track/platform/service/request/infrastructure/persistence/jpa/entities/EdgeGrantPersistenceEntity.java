package pe.edu.upc.ice.track.platform.service.request.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;

/**
 * JPA persistence entity for an {@code EdgeGrant}, mapped to the {@code edge_grants} table.
 *
 * <p>Two lookups are served: the unrevoked grants of a request, when it is reassigned, canceled or
 * completed, and the active grants of a unit, on every configuration poll of its gateway. The token
 * digest is unique, so two grants can never answer to the same token.</p>
 */
@Getter
@Setter
@Entity
@Table(
    name = "edge_grants",
    indexes = {
        @Index(name = "idx_edge_grants_token_digest", columnList = "token_digest", unique = true),
        @Index(name = "idx_edge_grants_service_request_id", columnList = "service_request_id"),
        @Index(name = "idx_edge_grants_equipment_id", columnList = "equipment_id")})
public class EdgeGrantPersistenceEntity extends AuditableAbstractPersistenceEntity {

  @Column(name = "service_request_id", nullable = false)
  private Long serviceRequestId;

  @Column(name = "technician_profile_id", nullable = false)
  private Long technicianProfileId;

  @Column(name = "equipment_id", nullable = false)
  private Long equipmentId;

  /** Hex SHA-256 digest of the grant token; the plaintext never appears in this entity. */
  @Column(name = "token_digest", nullable = false, length = 64)
  private String tokenDigest;

  @Column(name = "issued_at", nullable = false)
  private Instant issuedAt;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "revocation_reason", length = 30)
  private String revocationReason;

  public EdgeGrantPersistenceEntity() {
  }
}
