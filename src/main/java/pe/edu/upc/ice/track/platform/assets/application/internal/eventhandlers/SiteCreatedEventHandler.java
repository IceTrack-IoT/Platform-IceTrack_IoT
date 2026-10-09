package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;
import pe.edu.upc.ice.track.platform.assets.interfaces.events.SiteCreatedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link SiteCreatedEvent} domain event.
 *
 * <p>Translates the internal domain event into a {@link SiteCreatedIntegrationEvent} and
 * re-publishes it on the Spring event bus for cross-context consumers.</p>
 */
@Service
@Slf4j
public class SiteCreatedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher Spring application event publisher
   */
  public SiteCreatedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link SiteCreatedEvent} and publishes the corresponding
   * {@link SiteCreatedIntegrationEvent}.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(SiteCreatedEvent event) {
    log.info("Site created: id={}, ownerId={}, name='{}'",
        event.siteId(), event.ownerId(), event.name());
    eventPublisher.publishEvent(new SiteCreatedIntegrationEvent(
        event.siteId(), event.ownerId(), event.name()));
  }
}