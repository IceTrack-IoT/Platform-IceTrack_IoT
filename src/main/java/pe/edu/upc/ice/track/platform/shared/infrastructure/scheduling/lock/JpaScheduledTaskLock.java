package pe.edu.upc.ice.track.platform.shared.infrastructure.scheduling.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.shared.application.scheduling.ScheduledTaskLock;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapter for {@link ScheduledTaskLock} backed by the {@code scheduled_task_locks} table.
 *
 * <p>Each attempt runs in a transaction of its own and commits at once, so the lease is visible to
 * the other instances before the task starts. The expired lease is taken with one conditional
 * {@code UPDATE}; the first run of a task, when its row does not exist yet, inserts it, and the
 * primary key settles a race between two instances inserting at once.</p>
 *
 * <p>Instants come from each instance's clock, so the instances' clocks must agree to well within the
 * lease; a lease a few seconds shorter than the task's interval absorbs ordinary drift.</p>
 */
@Component
@Slf4j
public class JpaScheduledTaskLock implements ScheduledTaskLock {

  /** Identifies this instance in the lease rows; random, so two replicas never share it. */
  private final String instanceId = "instance-" + UUID.randomUUID();

  private final ScheduledTaskLockPersistenceRepository repository;
  private final TransactionTemplate transactionTemplate;

  public JpaScheduledTaskLock(ScheduledTaskLockPersistenceRepository repository, PlatformTransactionManager transactionManager) {
    this.repository = repository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
    this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  // inherited javadoc
  @Override
  public boolean tryAcquire(String taskName, Duration leaseFor) {
    Objects.requireNonNull(taskName, "taskName must not be null");
    if (leaseFor == null || leaseFor.isNegative() || leaseFor.isZero()) {
      throw new IllegalArgumentException("leaseFor must be positive");
    }
    var now = Instant.now();
    var lockedUntil = now.plus(leaseFor);
    try {
      return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
        var updated = repository.update(
            ScheduledTaskLockPersistenceSpecifications.takeLease(taskName, now, lockedUntil, instanceId));
        if (updated > 0) {
          return true;
        }
        if (repository.existsById(taskName)) {
          return false;
        }
        repository.saveAndFlush(new ScheduledTaskLockPersistenceEntity(taskName, lockedUntil, now, instanceId));
        return true;
      }));
    } catch (DataIntegrityViolationException e) {
      log.debug("Another instance took the first lease of task {}", taskName);
      return false;
    }
  }
}
