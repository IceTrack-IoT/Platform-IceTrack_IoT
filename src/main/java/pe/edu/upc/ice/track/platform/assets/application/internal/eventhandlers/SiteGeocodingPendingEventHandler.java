package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.assets.application.commandservices.SiteCommandService;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.commands.ResumeSiteGeocodingCommand;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteGeocodingPendingEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Duration;
import java.util.List;

/**
 * Continues locating a site that a geocoding retry left {@code PENDING} because the maps service
 * could not answer right away.
 *
 * <p>Runs asynchronously and only after the pending status is committed, so the owner's request is
 * answered at once and every attempt reads the committed row. Each attempt waits a little longer
 * than the one before. The last attempt never leaves the site {@code PENDING}: a provider that still
 * cannot answer leaves it {@code FAILED} with {@code PROVIDER_UNAVAILABLE}, from where the owner may
 * retry again. A site located or re-addressed meanwhile ends the loop early.</p>
 */
@Service
@Slf4j
public class SiteGeocodingPendingEventHandler {

  /** Wait before each background attempt. */
  static final List<Duration> ATTEMPT_DELAYS = List.of(Duration.ofSeconds(5), Duration.ofSeconds(20), Duration.ofSeconds(60));

  private final SiteCommandService siteCommandService;

  /**
   * Constructor
   *
   * @param siteCommandService the service recording each attempt's outcome
   */
  public SiteGeocodingPendingEventHandler(SiteCommandService siteCommandService) {
    this.siteCommandService = siteCommandService;
  }

  /**
   * Runs the background attempts for a pending site.
   *
   * @param event the domain event naming the site
   */
  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(SiteGeocodingPendingEvent event) {
    for (var attempt = 0; attempt < ATTEMPT_DELAYS.size(); attempt++) {
      if (!pause(ATTEMPT_DELAYS.get(attempt))) {
        return;
      }
      var finalAttempt = attempt == ATTEMPT_DELAYS.size() - 1;
      var stillPending = switch (siteCommandService.handle(new ResumeSiteGeocodingCommand(event.siteId(), finalAttempt))) {
        case Result.Success<Site, ApplicationError> success -> success.value().getGeocodingStatus() == GeocodingStatus.PENDING;
        case Result.Failure<Site, ApplicationError> failure -> false;
      };
      if (!stillPending) {
        return;
      }
      log.info("Site {} is still waiting for the maps service after background attempt {}", event.siteId(), attempt + 1);
    }
  }

  /**
   * Waits before the next attempt.
   *
   * @param delay how long to wait
   * @return {@code false} when the thread was interrupted, so no further attempt should run
   */
  private static boolean pause(Duration delay) {
    try {
      Thread.sleep(delay);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Background geocoding interrupted; the site stays PENDING until the owner retries");
      return false;
    }
  }
}
