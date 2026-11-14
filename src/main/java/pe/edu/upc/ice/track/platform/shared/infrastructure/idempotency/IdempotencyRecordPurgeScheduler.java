package pe.edu.upc.ice.track.platform.shared.infrastructure.idempotency;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes the idempotency records whose 24-hour retention has elapsed, once an hour.
 *
 * <p>An expired record is already ignored when its key comes back, so this only keeps the table
 * small. Running it on several instances at once is harmless: the purge is an idempotent bulk delete.</p>
 */
@Component
@Slf4j
public class IdempotencyRecordPurgeScheduler {

  private final IdempotencyRecordStore store;

  public IdempotencyRecordPurgeScheduler(IdempotencyRecordStore store) {
    this.store = store;
  }

  /**
   * Purges the expired records. A failure is logged and swallowed, so the next run still takes place.
   */
  @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
  public void purgeExpiredRecords() {
    try {
      var purged = store.purgeExpired();
      if (purged > 0) {
        log.info("Purged {} expired idempotency records", purged);
      }
    } catch (RuntimeException ex) {
      log.error("Purging expired idempotency records failed: {}", ex.getMessage(), ex);
    }
  }
}
