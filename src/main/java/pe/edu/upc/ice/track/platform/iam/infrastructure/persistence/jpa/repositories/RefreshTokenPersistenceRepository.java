package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.RefreshTokenPersistenceEntity;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * Spring Data repository for IAM refresh token persistence entities.
 *
 * <p>State changes are bulk statements: they flush pending writes first and clear the persistence
 * context afterward, so no stale managed instance can survive them.</p>
 */
@Repository
public interface RefreshTokenPersistenceRepository extends JpaRepository<RefreshTokenPersistenceEntity, Long> {

  /**
   * This method is responsible for finding the refresh token by its digest.
   * @param token The token digest.
   * @return The refresh token object.
   */
  Optional<RefreshTokenPersistenceEntity> findByToken(String token);

  /**
   * This method is responsible for rotating the refresh token matching a digest, if it is still active.
   * @param token The token digest.
   * @param replacedByToken The digest of the replacing token.
   * @param revokedAt The rotation instant to record.
   * @param updatedAt The modification timestamp to record.
   * @return The number of rotated tokens, 0 when the token was already revoked or does not exist.
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update RefreshTokenPersistenceEntity t set t.revoked = true, t.revokedAt = :revokedAt, "
      + "t.replacedByToken = :replacedByToken, t.updatedAt = :updatedAt "
      + "where t.token = :token and t.revoked = false")
  int rotateActiveByToken(
      @Param("token") String token,
      @Param("replacedByToken") String replacedByToken,
      @Param("revokedAt") Instant revokedAt,
      @Param("updatedAt") Date updatedAt);

  /**
   * This method is responsible for revoking the refresh token matching a digest, if it is still active.
   * @param token The token digest.
   * @param revokedAt The revocation instant to record.
   * @param updatedAt The modification timestamp to record.
   * @return The number of revoked tokens, 0 when the token was already revoked or does not exist.
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update RefreshTokenPersistenceEntity t set t.revoked = true, t.revokedAt = :revokedAt, "
      + "t.updatedAt = :updatedAt where t.token = :token and t.revoked = false")
  int revokeActiveByToken(
      @Param("token") String token, @Param("revokedAt") Instant revokedAt, @Param("updatedAt") Date updatedAt);

  /**
   * This method is responsible for revoking every active refresh token of an account.
   *
   * @param userId    The account identifier.
   * @param revokedAt The revocation instant to record.
   * @param updatedAt The modification timestamp to record.
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("update RefreshTokenPersistenceEntity t set t.revoked = true, t.revokedAt = :revokedAt, "
      + "t.updatedAt = :updatedAt where t.userId = :userId and t.revoked = false")
  void revokeAllActiveByUserId(
      @Param("userId") Long userId, @Param("revokedAt") Instant revokedAt, @Param("updatedAt") Date updatedAt);

  /**
   * This method is responsible for deleting every refresh token that expired before an instant.
   *
   * @param instant The reference instant.
   * @return The number of deleted tokens.
   */
  @Transactional
  @Modifying(flushAutomatically = true, clearAutomatically = true)
  @Query("delete from RefreshTokenPersistenceEntity t where t.expiryDate < :instant")
  int deleteAllExpiredBefore(@Param("instant") Instant instant);
}
