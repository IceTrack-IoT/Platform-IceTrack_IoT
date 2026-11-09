package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.specifications;

import org.springframework.data.jpa.domain.DeleteSpecification;
import org.springframework.data.jpa.domain.UpdateSpecification;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.RefreshTokenPersistenceEntity;

import java.time.Instant;
import java.util.Objects;

/**
 * Criteria API bulk update and delete specifications over {@link RefreshTokenPersistenceEntity}.
 *
 * <p>Every state change is a single conditional {@code UPDATE} rather than a load-modify-save, on
 * purpose: the {@code revoked = false} predicate makes each of them an atomic compare-and-set, so
 * when the same token is presented concurrently, the database lets exactly one request rotate or
 * revoke it and the others see {@code 0} affected rows. The auditing listener does not run for a
 * bulk statement, which is why each update also sets {@code updatedAt} itself.</p>
 */
public final class RefreshTokenPersistenceSpecifications {

  private static final String TOKEN = "token";
  private static final String USER_ID = "userId";
  private static final String EXPIRY_DATE = "expiryDate";
  private static final String REVOKED = "revoked";
  private static final String REVOKED_AT = "revokedAt";
  private static final String REPLACED_BY_TOKEN = "replacedByToken";
  private static final String UPDATED_AT = "updatedAt";

  private RefreshTokenPersistenceSpecifications() {
  }

  /**
   * Rotates the token matching a digest, provided it is still active.
   *
   * @param token           the token digest; required
   * @param replacedByToken the digest of the replacing token; required
   * @param revokedAt       the rotation instant to record; required
   * @param updatedAt       the modification timestamp to record; required
   * @return the update, affecting one row at most
   */
  public static UpdateSpecification<RefreshTokenPersistenceEntity> rotateActiveByToken(
      String token, String replacedByToken, Instant revokedAt, Instant updatedAt) {
    Objects.requireNonNull(token, "token must not be null");
    Objects.requireNonNull(replacedByToken, "replacedByToken must not be null");
    return (root, update, criteriaBuilder) -> {
      update.set(root.<Boolean>get(REVOKED), Boolean.TRUE);
      update.set(root.<Instant>get(REVOKED_AT), revokedAt);
      update.set(root.<String>get(REPLACED_BY_TOKEN), replacedByToken);
      update.set(root.<Instant>get(UPDATED_AT), updatedAt);
      return criteriaBuilder.and(
          criteriaBuilder.equal(root.get(TOKEN), token),
          criteriaBuilder.isFalse(root.<Boolean>get(REVOKED)));
    };
  }

  /**
   * Revokes the token matching a digest, provided it is still active, without recording a
   * replacement.
   *
   * @param token     the token digest; required
   * @param revokedAt the revocation instant to record; required
   * @param updatedAt the modification timestamp to record; required
   * @return the update, affecting one row at most
   */
  public static UpdateSpecification<RefreshTokenPersistenceEntity> revokeActiveByToken(
      String token, Instant revokedAt, Instant updatedAt) {
    Objects.requireNonNull(token, "token must not be null");
    return (root, update, criteriaBuilder) -> {
      update.set(root.<Boolean>get(REVOKED), Boolean.TRUE);
      update.set(root.<Instant>get(REVOKED_AT), revokedAt);
      update.set(root.<Instant>get(UPDATED_AT), updatedAt);
      return criteriaBuilder.and(
          criteriaBuilder.equal(root.get(TOKEN), token),
          criteriaBuilder.isFalse(root.<Boolean>get(REVOKED)));
    };
  }

  /**
   * Revokes every active token of an account.
   *
   * @param userId    the account identifier; required
   * @param revokedAt the revocation instant to record; required
   * @param updatedAt the modification timestamp to record; required
   * @return the update
   */
  public static UpdateSpecification<RefreshTokenPersistenceEntity> revokeAllActiveByUserId(
      Long userId, Instant revokedAt, Instant updatedAt) {
    Objects.requireNonNull(userId, "userId must not be null");
    return (root, update, criteriaBuilder) -> {
      update.set(root.<Boolean>get(REVOKED), Boolean.TRUE);
      update.set(root.<Instant>get(REVOKED_AT), revokedAt);
      update.set(root.<Instant>get(UPDATED_AT), updatedAt);
      return criteriaBuilder.and(
          criteriaBuilder.equal(root.get(USER_ID), userId),
          criteriaBuilder.isFalse(root.<Boolean>get(REVOKED)));
    };
  }

  /**
   * Deletes every token that expired strictly before an instant.
   *
   * @param instant the reference instant; required
   * @return the delete
   */
  public static DeleteSpecification<RefreshTokenPersistenceEntity> expiredBefore(Instant instant) {
    Objects.requireNonNull(instant, "instant must not be null");
    return (root, delete, criteriaBuilder) -> criteriaBuilder.lessThan(root.<Instant>get(EXPIRY_DATE), instant);
  }
}
