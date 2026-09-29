package pe.edu.upc.ice.track.platform.profiles.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.OwnerCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.interfaces.events.OwnerCreatedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link OwnerCreatedEvent} domain event.
 *
 * <p>Translates the internal domain event into an {@link OwnerCreatedIntegrationEvent} and
 * re-publishes it on the Spring event bus for cross-context consumers.</p>
 */
@Service
public class OwnerCreatedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher Spring application event publisher
   */
  public OwnerCreatedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link OwnerCreatedEvent} and publishes the corresponding
   * {@link OwnerCreatedIntegrationEvent}.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(OwnerCreatedEvent event) {
    eventPublisher.publishEvent(new OwnerCreatedIntegrationEvent(
        event.ownerId(),
        event.userId(),
        event.fullName(),
        event.email(),
        event.ruc()));
  }
}
