package pe.edu.upc.ice.track.platform.shared.infrastructure.scheduling.lock;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * JPA entity of the {@code scheduled_task_locks} table: one row per scheduled task, holding the lease
 * of the instance currently allowed to run it.
 */
@Entity
@Table(name = "scheduled_task_locks")
@Getter
@Setter
@NoArgsConstructor
public class ScheduledTaskLockPersistenceEntity {

  /** Name of the task. */
  @Id
  @Column(name = "name", length = 100, nullable = false)
  private String name;

  /** When the lease ends; another instance may take it from then on. */
  @Column(name = "locked_until", nullable = false)
  private Instant lockedUntil;

  /** When the lease was last taken. */
  @Column(name = "locked_at", nullable = false)
  private Instant lockedAt;

  /** The instance that holds, or last held, the lease. */
  @Column(name = "locked_by", length = 255, nullable = false)
  private String lockedBy;

  /**
   * Creates the row of a task taken for the first time.
   *
   * @param name        name of the task
   * @param lockedUntil when the lease ends
   * @param lockedAt    when the lease was taken
   * @param lockedBy    the instance taking it
   */
  public ScheduledTaskLockPersistenceEntity(String name, Instant lockedUntil, Instant lockedAt, String lockedBy) {
    this.name = name;
    this.lockedUntil = lockedUntil;
    this.lockedAt = lockedAt;
    this.lockedBy = lockedBy;
  }
}
