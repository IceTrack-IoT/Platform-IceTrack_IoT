package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertRaisedEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.AlertRaisedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link AlertRaisedEvent} domain event.
 *
 * <p>Translates the internal domain event into an {@link AlertRaisedIntegrationEvent} and
 * re-publishes it on the Spring event bus. This is the only place where a domain event crosses
 * the boundary between the domain layer and the published language of the {@code monitoring}
 * bounded context.</p>
 *
 * <p>Other bounded contexts must subscribe to {@link AlertRaisedIntegrationEvent} (from
 * {@code monitoring.interfaces.events}), never to the internal {@link AlertRaisedEvent}.</p>
 */
@Service("monitoringAlertRaisedEventHandler")
public class AlertRaisedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  public AlertRaisedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link AlertRaisedEvent} and publishes the corresponding
   * {@link AlertRaisedIntegrationEvent} for cross-context consumers, primarily Notifications.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(AlertRaisedEvent event) {
    eventPublisher.publishEvent(new AlertRaisedIntegrationEvent(
        event.alertId(),
        event.equipmentId(),
        event.type().name(),
        event.severity().name(),
        event.peakTemperature()));
  }
}
