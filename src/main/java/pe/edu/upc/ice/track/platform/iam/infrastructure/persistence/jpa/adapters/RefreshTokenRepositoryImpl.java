package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.RefreshToken;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RefreshTokenRepository;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers.RefreshTokenPersistenceAssembler;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories.RefreshTokenPersistenceRepository;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Repository adapter that bridges the IAM refresh token domain repository port with Spring Data JPA.
 */
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenPersistenceRepository refreshTokenPersistenceRepository;

  public RefreshTokenRepositoryImpl(RefreshTokenPersistenceRepository refreshTokenPersistenceRepository) {
    this.refreshTokenPersistenceRepository = refreshTokenPersistenceRepository;
  }

  @Override
  public Optional<RefreshToken> findByToken(String token) {
    if (token == null || token.isBlank()) return Optional.empty();
    return refreshTokenPersistenceRepository.findByToken(token)
        .map(RefreshTokenPersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    var saved = refreshTokenPersistenceRepository.save(
        RefreshTokenPersistenceAssembler.toPersistenceFromDomain(refreshToken));
    return RefreshTokenPersistenceAssembler.toDomainFromPersistence(saved);
  }

  @Override
  public boolean revoke(String token) {
    if (token == null || token.isBlank()) return false;
    var now = Instant.now();
    return refreshTokenPersistenceRepository.revokeActiveByToken(token, now, Date.from(now)) > 0;
  }

  @Override
  public void revokeAllByUserId(Long userId) {
    if (userId == null) return;
    var now = Instant.now();
    refreshTokenPersistenceRepository.revokeAllActiveByUserId(userId, now, Date.from(now));
  }

  @Override
  public int deleteAllExpiredBefore(Instant instant) {
    if (instant == null) return 0;
    return refreshTokenPersistenceRepository.deleteAllExpiredBefore(instant);
  }

  @Override
  public void deleteByToken(String token) {
    if (token == null || token.isBlank()) return;
    refreshTokenPersistenceRepository.deleteByToken(token);
  }
}
