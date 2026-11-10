package pe.edu.upc.ice.track.platform.shared.application.scheduling;

import java.time.Duration;

/**
 * Outbound port through which a scheduled task makes sure it runs on one instance at a time.
 *
 * <p>A task that every replica would otherwise run at once - and race on the same rows - takes a
 * lease before each run and skips the run when another instance holds it. The lease expires on its
 * own, so an instance that dies mid-run never blocks the task for longer than the lease.</p>
 */
public interface ScheduledTaskLock {

  /**
   * Takes the lease of a task, unless another instance holds it and it has not expired yet.
   *
   * @param taskName the name of the task; required
   * @param leaseFor how long the lease lasts; shorter than the task's interval, so the next run can
   *                 take it again; required and positive
   * @return {@code true} when this instance holds the lease and should run the task
   */
  boolean tryAcquire(String taskName, Duration leaseFor);
}
