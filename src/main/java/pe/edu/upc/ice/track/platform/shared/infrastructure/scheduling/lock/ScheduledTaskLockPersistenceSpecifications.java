package pe.edu.upc.ice.track.platform.shared.infrastructure.scheduling.lock;

import org.springframework.data.jpa.domain.UpdateSpecification;

import java.time.Instant;
import java.util.Objects;

/**
 * Criteria API update specifications over {@link ScheduledTaskLockPersistenceEntity}.
 *
 * <p>Taking a lease is a single conditional {@code UPDATE}: the row is only changed when its lease
 * has expired or already belongs to the instance asking, so when two instances race for the same
 * task the database lets exactly one of them change the row.</p>
 */
public final class ScheduledTaskLockPersistenceSpecifications {

  private static final String NAME = "name";
  private static final String LOCKED_UNTIL = "lockedUntil";
  private static final String LOCKED_AT = "lockedAt";
  private static final String LOCKED_BY = "lockedBy";

  private ScheduledTaskLockPersistenceSpecifications() {
  }

  /**
   * Takes the lease of a task when it has expired or is already held by the same instance.
   *
   * @param name        name of the task; required
   * @param now         the current instant; required
   * @param lockedUntil when the new lease ends; required
   * @param lockedBy    the instance taking the lease; required
   * @return the update, affecting one row at most
   */
  public static UpdateSpecification<ScheduledTaskLockPersistenceEntity> takeLease(
      String name, Instant now, Instant lockedUntil, String lockedBy) {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(now, "now must not be null");
    Objects.requireNonNull(lockedUntil, "lockedUntil must not be null");
    Objects.requireNonNull(lockedBy, "lockedBy must not be null");
    return (root, update, criteriaBuilder) -> {
      update.set(root.<Instant>get(LOCKED_UNTIL), lockedUntil);
      update.set(root.<Instant>get(LOCKED_AT), now);
      update.set(root.<String>get(LOCKED_BY), lockedBy);
      return criteriaBuilder.and(
          criteriaBuilder.equal(root.get(NAME), name),
          criteriaBuilder.or(
              criteriaBuilder.lessThanOrEqualTo(root.<Instant>get(LOCKED_UNTIL), now),
              criteriaBuilder.equal(root.get(LOCKED_BY), lockedBy)));
    };
  }
}
