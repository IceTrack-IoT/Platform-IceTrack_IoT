package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingStatus;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.util.List;
import java.util.stream.Stream;

/**
 * Application lifecycle handler that re-queues the geocoding of every site left {@code PENDING}
 * or {@code FAILED} when the application starts.
 *
 * <p>Geocoding is triggered by an in-memory event, so a lookup queued or running when the
 * application stopped is lost with it, and sites that existed before geocoding was introduced
 * start out {@code PENDING} without ever having had a lookup. {@code FAILED} sites are included
 * because a restart is typically what follows fixing the cause - a missing API key, a provider
 * outage. {@code NOT_FOUND} sites are not: Google already answered for that address, and only a
 * new address can change the answer.</p>
 *
 * <p>Each site is re-queued through {@link Site#retryGeocoding()} in its own short transaction,
 * so the lookup takes exactly the path a new address takes: dispatched asynchronously once that
 * transaction commits. Nothing here may stop the application from starting: a failure is logged
 * and the site waits for the next start.</p>
 */
@Service
@Slf4j
public class SiteGeocodingRecoveryEventHandler {

  private final SiteRepository siteRepository;
  private final TransactionTemplate transactionTemplate;

  /**
   * Constructor
   *
   * @param siteRepository     the {@link SiteRepository} instance
   * @param transactionManager the transaction manager each site is re-queued with
   */
  public SiteGeocodingRecoveryEventHandler(
      SiteRepository siteRepository, PlatformTransactionManager transactionManager) {
    this.siteRepository = siteRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * Re-queues a lookup for every site left pending or failed.
   *
   * @param event Spring Boot readiness event
   */
  @EventListener
  public void on(ApplicationReadyEvent event) {
    List<Long> siteIds;
    try {
      siteIds = Stream.of(GeocodingStatus.PENDING, GeocodingStatus.FAILED)
          .flatMap(status -> siteRepository.findByGeocodingStatus(status).stream())
          .map(Site::getSiteId)
          .toList();
    } catch (RuntimeException e) {
      log.error("Could not look up sites awaiting geocoding; they will be resumed on the next start", e);
      return;
    }
    if (siteIds.isEmpty()) {
      return;
    }
    log.info("Re-queuing geocoding of {} site(s) left pending or failed", siteIds.size());
    for (var siteId : siteIds) {
      try {
        transactionTemplate.executeWithoutResult(status -> siteRepository.findById(siteId).ifPresent(site -> {
          site.retryGeocoding();
          siteRepository.save(site);
        }));
      } catch (RuntimeException e) {
        log.warn("Could not re-queue geocoding of site {}; it will be retried on the next start", siteId, e);
      }
    }
  }
}
