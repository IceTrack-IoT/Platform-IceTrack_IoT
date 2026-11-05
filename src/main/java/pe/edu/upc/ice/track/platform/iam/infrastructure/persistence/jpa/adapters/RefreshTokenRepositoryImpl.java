package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.adapters;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.RefreshToken;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RefreshTokenRepository;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers.RefreshTokenPersistenceAssembler;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories.RefreshTokenPersistenceRepository;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.specifications.RefreshTokenPersistenceSpecifications;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.function.LongSupplier;

/**
 * Repository adapter that bridges the IAM refresh token domain repository port with Spring Data JPA.
 *
 * <p>Every state change is a bulk statement built by {@link RefreshTokenPersistenceSpecifications}.
 * A bulk statement bypasses the persistence context, so each one runs through
 * {@link #bulk(LongSupplier)}: pending writes are flushed first, so the statement sees them, and
 * the context is cleared afterward, so no managed instance loaded earlier in the transaction keeps
 * a state the statement has since overwritten.</p>
 */
@Repository
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

  private final RefreshTokenPersistenceRepository refreshTokenPersistenceRepository;
  private final EntityManager entityManager;

  public RefreshTokenRepositoryImpl(
      RefreshTokenPersistenceRepository refreshTokenPersistenceRepository, EntityManager entityManager) {
    this.refreshTokenPersistenceRepository = refreshTokenPersistenceRepository;
    this.entityManager = entityManager;
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
  @Transactional
  public boolean saveRotation(RefreshToken rotatedToken) {
    if (rotatedToken == null || !rotatedToken.isRotated()) return false;
    var rotation = RefreshTokenPersistenceSpecifications.rotateActiveByToken(
        rotatedToken.getToken(),
        rotatedToken.getReplacedByToken(),
        rotatedToken.getRevokedAt(),
        Date.from(rotatedToken.getRevokedAt()));
    return bulk(() -> refreshTokenPersistenceRepository.update(rotation)) > 0;
  }

  @Override
  @Transactional
  public boolean revoke(String token) {
    if (token == null || token.isBlank()) return false;
    var now = Instant.now();
    var revocation = RefreshTokenPersistenceSpecifications.revokeActiveByToken(token, now, Date.from(now));
    return bulk(() -> refreshTokenPersistenceRepository.update(revocation)) > 0;
  }

  @Override
  @Transactional
  public void revokeAllByUserId(Long userId) {
    if (userId == null) return;
    var now = Instant.now();
    var revocation = RefreshTokenPersistenceSpecifications.revokeAllActiveByUserId(userId, now, Date.from(now));
    bulk(() -> refreshTokenPersistenceRepository.update(revocation));
  }

  @Override
  @Transactional
  public int deleteAllExpiredBefore(Instant instant) {
    if (instant == null) return 0;
    var purge = RefreshTokenPersistenceSpecifications.expiredBefore(instant);
    return Math.toIntExact(bulk(() -> refreshTokenPersistenceRepository.delete(purge)));
  }

  /**
   * Runs a bulk statement between a flush and a clear of the persistence context, which is what
   * {@code @Modifying(flushAutomatically = true, clearAutomatically = true)} used to do.
   *
   * @param statement the bulk statement, returning the number of affected rows
   * @return the number of affected rows
   */
  private long bulk(LongSupplier statement) {
    entityManager.flush();
    try {
      return statement.getAsLong();
    } finally {
      entityManager.clear();
    }
  }
}
