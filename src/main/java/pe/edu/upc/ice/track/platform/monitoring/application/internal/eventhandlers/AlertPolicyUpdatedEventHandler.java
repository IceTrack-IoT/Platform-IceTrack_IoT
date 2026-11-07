package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertPolicyUpdatedEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.AlertPolicyUpdatedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link AlertPolicyUpdatedEvent} domain event.
 *
 * <p>Translates it into an {@link AlertPolicyUpdatedIntegrationEvent} for other contexts. It runs
 * synchronously, inside the transaction that saved the policy, so the configuration version of the
 * unit is incremented in that same transaction.</p>
 */
@Service
public class AlertPolicyUpdatedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  public AlertPolicyUpdatedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Publishes the integration event matching an updated policy.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(AlertPolicyUpdatedEvent event) {
    eventPublisher.publishEvent(new AlertPolicyUpdatedIntegrationEvent(
        event.equipmentId(),
        event.sustainedExcursionMinutes(),
        event.hysteresisMarginCelsius(),
        event.missedSyncWindowsForOffline()));
  }
}
