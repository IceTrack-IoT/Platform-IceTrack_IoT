package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;

import java.time.Instant;

/**
 * JPA persistence entity for IAM refresh tokens.
 */
@Entity
@Table(
    name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"),
        // Serves the scheduled purge of expired tokens.
        @Index(name = "idx_refresh_tokens_expiry_date", columnList = "expiry_date")
    })
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenPersistenceEntity extends AuditableAbstractPersistenceEntity {

  /**
   * SHA-256 digest of the raw token, hex encoded. The raw value only ever exists on the client.
   */
  @Column(name = "token", nullable = false, unique = true, length = 64)
  private String token;

  /**
   * Identifier of the account the session belongs to. Mapped as a plain column rather than an
   * association: the user aggregate is referenced by identity and never loaded through a token.
   */
  @Column(name = "user_id", nullable = false)
  private Long userId;

  @Column(name = "expiry_date", nullable = false)
  private Instant expiryDate;

  @Column(name = "revoked", nullable = false)
  private boolean revoked;

  /**
   * Instant the token was revoked. Nullable: it is empty while the token is active, and for rows
   * revoked before the column was introduced.
   */
  @Column(name = "revoked_at")
  private Instant revokedAt;

  /**
   * SHA-256 digest of the token that replaced this one when it was rotated - never the raw value,
   * which would let a leaked row be exchanged for a live session. Null when the token was not
   * rotated, which tells an explicit revocation apart from a rotation.
   */
  @Column(name = "replaced_by_token", length = 64)
  private String replacedByToken;
}
