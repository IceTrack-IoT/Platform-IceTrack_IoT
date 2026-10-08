package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.SiteCreatedEvent;

/**
 * Internal application-layer handler for the {@link SiteCreatedEvent} domain event.
 *
 * <p>This context has no outbound integration to perform when a site is registered - the site only
 * becomes visible to the rest of the platform through its own queries - so the handler observes and
 * traces the event instead of translating it. It exists because an event nobody listens to is an
 * event nobody can tell apart from one that never fired, and a dropped registration is exactly the
 * kind of failure that is invisible until somebody notices a missing site.</p>
 */
@Component
@Slf4j
public class SiteCreatedEventHandler {

  /**
   * Receives the internal {@link SiteCreatedEvent} once the owning transaction has committed, so
   * the trace line can never advertise a site that was rolled back.
   *
   * @param event the internal domain event
   */
  @TransactionalEventListener
  public void on(SiteCreatedEvent event) {
    log.info("Site created: id={}, ownerId={}, name='{}'",
        event.siteId(), event.ownerId(), event.name());
  }
}