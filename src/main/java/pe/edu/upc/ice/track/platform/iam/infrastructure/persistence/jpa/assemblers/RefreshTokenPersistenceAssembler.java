package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.iam.domain.model.entities.RefreshToken;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.RefreshTokenPersistenceEntity;

/**
 * Static assembler between IAM refresh token domain and persistence representations.
 */
public final class RefreshTokenPersistenceAssembler {

  private RefreshTokenPersistenceAssembler() {
  }

  public static RefreshToken toDomainFromPersistence(RefreshTokenPersistenceEntity entity) {
    if (entity == null) return null;
    return new RefreshToken(
        entity.getId(),
        entity.getUserId(),
        entity.getToken(),
        entity.getExpiryDate(),
        entity.isRevoked(),
        entity.getRevokedAt(),
        entity.getReplacedByToken());
  }

  public static RefreshTokenPersistenceEntity toPersistenceFromDomain(RefreshToken refreshToken) {
    if (refreshToken == null) return null;
    var entity = new RefreshTokenPersistenceEntity();
    // Only set ID if the token is being updated (has a non-null ID)
    // For new tokens, leave ID null to allow JPA to generate it
    if (refreshToken.getId() != null) {
      entity.setId(refreshToken.getId());
    }
    entity.setUserId(refreshToken.getUserId());
    entity.setToken(refreshToken.getToken());
    entity.setExpiryDate(refreshToken.getExpiryDate());
    entity.setRevoked(refreshToken.isRevoked());
    entity.setRevokedAt(refreshToken.getRevokedAt());
    entity.setReplacedByToken(refreshToken.getReplacedByToken());
    return entity;
  }
}
