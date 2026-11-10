package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.commandservices.EquipmentConnectivityCommandService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DetectOfflineEquipmentCommand;
import pe.edu.upc.ice.track.platform.shared.application.scheduling.ScheduledTaskLock;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Scheduled handler that runs the offline detection: every connected unit whose latest batch arrived
 * longer ago than its offline window is marked offline, and a {@code DEVICE_OFFLINE} alert is raised
 * for it, which notifies the owner and is pushed to the owner's stream.
 *
 * <p>It runs every {@code monitoring.offline-detection.fixed-delay-ms} (one synchronization window,
 * 60 seconds, by default), so a unit is flagged at most one window after its silence threshold. With
 * several instances only one runs each round: the run takes the {@value #TASK_NAME} lease of
 * {@link ScheduledTaskLock} first and is skipped when another instance holds it. The lease lasts
 * {@code monitoring.offline-detection.lock-lease-ms}, shorter than the interval, so the next round can
 * be taken by any instance, and a dead instance never blocks the detection for longer than that.</p>
 *
 * <p>Each run is logged with its outcome, and the instant of the last completed run is kept for
 * operations.</p>
 */
@Service
@Slf4j
public class OfflineDetectionScheduler {

  /** Name of the lease the run takes. */
  static final String TASK_NAME = "monitoring.offline-detection";

  private final EquipmentConnectivityCommandService equipmentConnectivityCommandService;
  private final ScheduledTaskLock scheduledTaskLock;
  private final boolean enabled;
  private final Duration lease;
  private final AtomicReference<Instant> lastCompletedRunAt = new AtomicReference<>();

  public OfflineDetectionScheduler(
      EquipmentConnectivityCommandService equipmentConnectivityCommandService,
      ScheduledTaskLock scheduledTaskLock,
      @Value("${monitoring.offline-detection.enabled:true}") boolean enabled,
      @Value("${monitoring.offline-detection.lock-lease-ms:50000}") long leaseMillis) {
    if (leaseMillis < 1) {
      throw new IllegalStateException("monitoring.offline-detection.lock-lease-ms must be positive");
    }
    this.equipmentConnectivityCommandService = equipmentConnectivityCommandService;
    this.scheduledTaskLock = scheduledTaskLock;
    this.enabled = enabled;
    this.lease = Duration.ofMillis(leaseMillis);
  }

  /**
   * Runs one round of the offline detection on this instance, unless another instance holds the
   * round. A failure is logged and swallowed, so the next round still takes place.
   */
  @Scheduled(
      fixedDelayString = "${monitoring.offline-detection.fixed-delay-ms:60000}",
      initialDelayString = "${monitoring.offline-detection.initial-delay-ms:60000}")
  public void detectOfflineEquipment() {
    if (!enabled) {
      return;
    }
    try {
      if (!scheduledTaskLock.tryAcquire(TASK_NAME, lease)) {
        log.debug("Offline detection skipped: another instance holds this round");
        return;
      }
      var startedAt = Instant.now();
      var raised = equipmentConnectivityCommandService.handle(new DetectOfflineEquipmentCommand(startedAt));
      lastCompletedRunAt.set(Instant.now());
      if (raised > 0) {
        log.info("Offline detection raised {} DEVICE_OFFLINE alert(s) in {} ms",
            raised, Duration.between(startedAt, lastCompletedRunAt.get()).toMillis());
      } else {
        log.debug("Offline detection completed in {} ms; no unit went silent",
            Duration.between(startedAt, lastCompletedRunAt.get()).toMillis());
      }
    } catch (RuntimeException exception) {
      log.error("Offline detection failed: {}", exception.getMessage(), exception);
    }
  }

  /**
   * @return when this instance last completed a round, or {@code null} when it has not yet
   */
  public Instant lastCompletedRunAt() {
    return lastCompletedRunAt.get();
  }
}
