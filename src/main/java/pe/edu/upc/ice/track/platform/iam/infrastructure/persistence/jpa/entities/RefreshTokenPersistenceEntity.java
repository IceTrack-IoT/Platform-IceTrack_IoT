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
    indexes = @Index(name = "idx_refresh_tokens_user_id", columnList = "user_id"))
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
}
