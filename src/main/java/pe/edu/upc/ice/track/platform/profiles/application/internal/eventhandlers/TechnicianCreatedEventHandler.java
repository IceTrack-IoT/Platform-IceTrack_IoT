package pe.edu.upc.ice.track.platform.profiles.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.TechnicianCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.interfaces.events.TechnicianCreatedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link TechnicianCreatedEvent} domain event.
 *
 * <p>Translates the internal domain event into a {@link TechnicianCreatedIntegrationEvent} and
 * re-publishes it on the Spring event bus for cross-context consumers.</p>
 */
@Service
public class TechnicianCreatedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher Spring application event publisher
   */
  public TechnicianCreatedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link TechnicianCreatedEvent} and publishes the corresponding
   * {@link TechnicianCreatedIntegrationEvent}.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(TechnicianCreatedEvent event) {
    eventPublisher.publishEvent(new TechnicianCreatedIntegrationEvent(
        event.technicianId(),
        event.userId(),
        event.fullName(),
        event.email(),
        event.speciality(),
        event.certificationNumber()));
  }
}
