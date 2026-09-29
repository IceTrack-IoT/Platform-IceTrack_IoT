package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.RefreshToken;
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
        entity.isRevoked());
  }

  public static RefreshTokenPersistenceEntity toPersistenceFromDomain(RefreshToken refreshToken) {
    if (refreshToken == null) return null;
    var entity = new RefreshTokenPersistenceEntity();
    // Only set ID if the token is being updated (has a non-null ID)
    // For new tokens, leave ID null to allow JPA to generate it
    if (refreshToken.id() != null) {
      entity.setId(refreshToken.id());
    }
    entity.setUserId(refreshToken.userId());
    entity.setToken(refreshToken.token());
    entity.setExpiryDate(refreshToken.expiryDate());
    entity.setRevoked(refreshToken.revoked());
    return entity;
  }
}
