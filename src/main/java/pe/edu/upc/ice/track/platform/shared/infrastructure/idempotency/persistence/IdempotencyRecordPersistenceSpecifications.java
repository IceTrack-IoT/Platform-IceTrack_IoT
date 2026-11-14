package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency.persistence;

import org.springframework.data.jpa.domain.DeleteSpecification;

import java.time.Instant;
import java.util.Objects;

/**
 * Criteria API delete specifications over {@link IdempotencyRecordPersistenceEntity}.
 */
public final class IdempotencyRecordPersistenceSpecifications {

  private static final String EXPIRES_AT = "expiresAt";

  private IdempotencyRecordPersistenceSpecifications() {
  }

  /**
   * Deletes every record that expired strictly before an instant.
   *
   * @param instant the reference instant; required
   * @return the delete
   */
  public static DeleteSpecification<IdempotencyRecordPersistenceEntity> expiredBefore(Instant instant) {
    Objects.requireNonNull(instant, "instant must not be null");
    return (root, delete, criteriaBuilder) -> criteriaBuilder.lessThan(root.<Instant>get(EXPIRES_AT), instant);
  }
}
