package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;
import pe.edu.upc.ice.track.platform.assets.application.internal.outboundservices.geocoding.GeocodingPort;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteAddressChangedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingOutcome;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.GeocodingResult;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Internal application-layer handler that locates a site's address once it has been saved (US-20,
 * TS-17).
 *
 * <p>Three properties keep the owner's request independent of the geocoding provider:</p>
 * <ul>
 *   <li>it is a {@link TransactionalEventListener}, so it only runs once the transaction that
 *       registered or changed the address has <em>committed</em> - a lookup never starts for a
 *       change that was rolled back, and nothing it does can roll that change back;</li>
 *   <li>it is {@link Async} on its own bounded pool, so the HTTP response has already been sent
 *       with {@code PENDING} by the time the provider is called;</li>
 *   <li>it never lets an exception escape, so a provider failure ends as a {@code FAILED} site and
 *       not as an error anybody else has to handle.</li>
 * </ul>
 *
 * <p>A {@code TRANSIENT_FAILURE} is retried up to {@value #MAX_RETRIES} times with exponential
 * backoff and jitter. The lookup itself runs outside any transaction; only recording its result
 * opens one, briefly, so no database connection is held while the provider is slow.</p>
 *
 * <p>Recording is guarded twice. The site must still be awaiting a lookup of <em>this</em> address
 * - otherwise the owner changed it meanwhile, or a newer lookup already settled it, and this result
 * is discarded. And the write is version-checked, so if the owner edits the site in the instant
 * between reading and writing it, the write is retried on fresh state instead of overwriting the
 * edit.</p>
 */
@Service
@Slf4j
public class SiteGeocodingEventHandler {

  /**
   * Retries after the first attempt, for transient failures only.
   */
  static final int MAX_RETRIES = 3;

  private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(1);
  private static final double BACKOFF_MULTIPLIER = 2.0;
  private static final Duration MAX_BACKOFF = Duration.ofSeconds(10);

  /**
   * Attempts at recording a result when the site keeps being modified concurrently.
   */
  private static final int MAX_WRITE_ATTEMPTS = 3;

  private final GeocodingPort geocodingPort;
  private final SiteRepository siteRepository;
  private final TransactionTemplate transactionTemplate;

  /**
   * Constructor
   *
   * @param geocodingPort      the {@link GeocodingPort} used to locate addresses
   * @param siteRepository     the {@link SiteRepository} instance
   * @param transactionManager the transaction manager the result is recorded with
   */
  public SiteGeocodingEventHandler(
      GeocodingPort geocodingPort,
      SiteRepository siteRepository,
      PlatformTransactionManager transactionManager) {
    this.geocodingPort = geocodingPort;
    this.siteRepository = siteRepository;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  /**
   * Locates the address carried by the event and records the outcome on the site.
   *
   * <p>The executor is named explicitly so lookups never run on, or starve, Spring Boot's default
   * {@code applicationTaskExecutor}; the name must match the bean declared by
   * {@code GeocodingExecutorConfiguration}. {@code fallbackExecution} makes an address changed
   * outside a transaction still get its lookup instead of being silently dropped.</p>
   *
   * @param event the domain event
   */
  @Async("geocodingTaskExecutor")
  @TransactionalEventListener(fallbackExecution = true)
  public void on(SiteAddressChangedEvent event) {
    try {
      var result = geocodeWithRetries(event);
      record(event, result);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      log.warn("Geocoding of site {} was interrupted; it stays PENDING and is resumed on the next start",
          event.siteId());
    } catch (RuntimeException e) {
      log.error("Geocoding result of site {} could not be recorded; it stays PENDING", event.siteId(), e);
    }
  }

  /**
   * Runs the lookup, retrying transient failures with exponential backoff and jitter.
   *
   * @param event the event naming the site and the address
   * @return the last result obtained; still {@code TRANSIENT_FAILURE} when every retry failed
   * @throws InterruptedException when the thread is interrupted while waiting to retry
   */
  private GeocodingResult geocodeWithRetries(SiteAddressChangedEvent event) throws InterruptedException {
    var result = geocode(event.address());
    for (var retry = 1; retry <= MAX_RETRIES && result.outcome() == GeocodingOutcome.TRANSIENT_FAILURE; retry++) {
      var delay = backoffBeforeRetry(retry);
      log.warn("Geocoding of site {} failed transiently; retry {}/{} in {} ms",
          event.siteId(), retry, MAX_RETRIES, delay.toMillis());
      Thread.sleep(delay);
      result = geocode(event.address());
    }
    return result;
  }

  /**
   * Calls the port, treating an adapter that throws despite its contract as a transient failure
   * rather than letting it leave the site in {@code PENDING}.
   *
   * @param address the address to locate
   * @return the result of the attempt
   */
  private GeocodingResult geocode(String address) {
    try {
      return geocodingPort.geocode(address);
    } catch (RuntimeException e) {
      log.warn("Geocoding adapter failed unexpectedly; treating it as transient", e);
      return GeocodingResult.transientFailure();
    }
  }

  /**
   * Computes the wait before a retry: exponential, capped, with "equal jitter".
   *
   * <p>Half of the delay is kept so that a rate-limited provider always gets some rest; the other
   * half is randomized so that lookups that failed together do not all retry together.</p>
   *
   * @param retry the retry about to happen, starting at 1
   * @return the delay to wait
   */
  static Duration backoffBeforeRetry(int retry) {
    var exponential = INITIAL_BACKOFF.toMillis() * Math.pow(BACKOFF_MULTIPLIER, retry - 1);
    var ceiling = (long) Math.min(exponential, MAX_BACKOFF.toMillis());
    var half = ceiling / 2;
    return Duration.ofMillis(half + ThreadLocalRandom.current().nextLong(half + 1));
  }

  /**
   * Records the result, re-reading the site when a concurrent edit made the first write stale.
   *
   * @param event  the event naming the site and the address
   * @param result the result to record
   */
  private void record(SiteAddressChangedEvent event, GeocodingResult result) {
    for (var attempt = 1; ; attempt++) {
      try {
        transactionTemplate.executeWithoutResult(status -> apply(event, result));
        return;
      } catch (OptimisticLockingFailureException e) {
        if (attempt >= MAX_WRITE_ATTEMPTS) {
          throw e;
        }
        log.info("Site {} changed while its geocoding result was being recorded; re-reading it", event.siteId());
      }
    }
  }

  /**
   * Applies the result to the site, provided the site is still waiting for it.
   *
   * @param event  the event naming the site and the address
   * @param result the result to apply
   */
  private void apply(SiteAddressChangedEvent event, GeocodingResult result) {
    var site = siteRepository.findById(event.siteId()).orElse(null);
    if (site == null) {
      log.info("Site {} no longer exists; its geocoding result is discarded", event.siteId());
      return;
    }
    if (!site.isAwaitingGeocodingOf(event.address())) {
      log.info("Site {} changed its address or was already geocoded; this result is discarded", event.siteId());
      return;
    }
    switch (result.outcome()) {
      case RESOLVED -> site.markLocationResolved(result.location());
      case NOT_FOUND -> site.markLocationNotFound();
      case TRANSIENT_FAILURE, PERMANENT_FAILURE -> site.markLocationFailed();
    }
    siteRepository.save(site);
    log.info("Geocoding of site {} settled as {}", event.siteId(), site.getGeocodingStatus());
  }
}
