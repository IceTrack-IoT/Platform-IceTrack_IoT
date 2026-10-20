package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;

/**
 * Internal application-layer handler for the {@link SiteCreatedEvent} domain event.
 */
@Service
@Slf4j
public class SiteCreatedEventHandler {

  /**
   * Receives the {@link SiteCreatedEvent} once the new site has been persisted.
   *
   * @param event the domain event
   */
  @EventListener
  public void on(SiteCreatedEvent event) {
    log.info("Site created: id={}, ownerId={}, name='{}'",
        event.siteId(), event.ownerId(), event.name());
  }
}
