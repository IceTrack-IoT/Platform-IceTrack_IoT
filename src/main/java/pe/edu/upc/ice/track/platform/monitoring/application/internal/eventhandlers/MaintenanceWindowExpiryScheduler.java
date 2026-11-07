package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices.MaintenanceWindowEventProcessor;

import java.time.Duration;
import java.time.Instant;

/**
 * Scheduled handler that closes maintenance windows whose gateway never reported their end.
 *
 * <p>Gateways close expired windows themselves and report it; a window still active
 * {@code monitoring.maintenance-window.expiry-grace-minutes} after its expiration belongs to a
 * gateway that went silent, and is closed as {@code EXPIRED} so its unit leaves maintenance and its
 * alerts are evaluated again. The sweep runs every
 * {@code monitoring.maintenance-window.expiry-sweep-delay-ms}. Running it on several instances at
 * once is harmless: closing a window twice changes nothing.</p>
 */
@Service
@Slf4j
public class MaintenanceWindowExpiryScheduler {

  private final MaintenanceWindowEventProcessor maintenanceWindowEventProcessor;
  private final Duration grace;

  public MaintenanceWindowExpiryScheduler(
      MaintenanceWindowEventProcessor maintenanceWindowEventProcessor,
      @Value("${monitoring.maintenance-window.expiry-grace-minutes:30}") long graceMinutes) {
    if (graceMinutes < 0) {
      throw new IllegalStateException("monitoring.maintenance-window.expiry-grace-minutes must not be negative");
    }
    this.maintenanceWindowEventProcessor = maintenanceWindowEventProcessor;
    this.grace = Duration.ofMinutes(graceMinutes);
  }

  /**
   * Closes the overdue windows. A failure is logged and swallowed, so the next run still takes place.
   */
  @Scheduled(fixedDelayString = "${monitoring.maintenance-window.expiry-sweep-delay-ms:60000}")
  public void closeOverdueWindows() {
    try {
      var closed = maintenanceWindowEventProcessor.closeExpired(Instant.now(), grace);
      if (closed > 0) {
        log.info("Closed {} overdue maintenance window(s)", closed);
      }
    } catch (RuntimeException exception) {
      log.error("Closing overdue maintenance windows failed: {}", exception.getMessage(), exception);
    }
  }
}
