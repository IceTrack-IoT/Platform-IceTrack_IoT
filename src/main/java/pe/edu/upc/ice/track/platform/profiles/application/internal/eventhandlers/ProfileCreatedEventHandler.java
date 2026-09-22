package pe.edu.upc.ice.track.platform.profiles.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.ProfileCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.interfaces.events.ProfileCreatedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link ProfileCreatedEvent} domain event.
 *
 * <p>Translates the internal domain event into a {@link ProfileCreatedIntegrationEvent}
 * and re-publishes it on the Spring event bus. This is the only place where a domain
 * event crosses the boundary between the domain layer and the published language of the
 * {@code profiles} bounded context.</p>
 *
 * <p>Other bounded contexts must subscribe to {@link ProfileCreatedIntegrationEvent}
 * (from {@code profiles.interfaces.events}), never to the internal
 * {@link ProfileCreatedEvent}.</p>
 */
@Service("profilesProfileCreatedEventHandler")
public class ProfileCreatedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher Spring application event publisher
   */
  public ProfileCreatedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link ProfileCreatedEvent} and publishes the
   * corresponding {@link ProfileCreatedIntegrationEvent} for cross-context consumers.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(ProfileCreatedEvent event) {
    eventPublisher.publishEvent(new ProfileCreatedIntegrationEvent(
        event.profileId(),
        event.userId(),
        event.firstName(),
        event.lastName(),
        event.email(),
        event.role(),
        event.countryCode(),
        event.phoneNumber(),
        event.street(),
        event.number(),
        event.city(),
        event.postalCode(),
        event.country()));
  }
}
