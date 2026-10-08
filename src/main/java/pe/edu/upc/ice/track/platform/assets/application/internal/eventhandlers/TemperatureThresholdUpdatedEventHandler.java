package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent;

/**
 * Internal application-layer handler for the {@link TemperatureThresholdUpdatedEvent} domain
 * event.
 *
 * <p>This is the event that matters most to the rest of the platform: it is the signal that a
 * unit's acceptable band moved, and Monitoring and Alerting must start evaluating new readings
 * against the new values. The event fires only for a threshold that was actually applied, so its
 * presence is proof that a rejection did not happen.</p>
 */
@Component
@Slf4j
public class TemperatureThresholdUpdatedEventHandler {

  /**
   * Receives the internal {@link TemperatureThresholdUpdatedEvent} after the transaction
   * committed.
   *
   * @param event the internal domain event
   */
  @TransactionalEventListener
  public void on(TemperatureThresholdUpdatedEvent event) {
    log.info("Temperature threshold updated: equipmentId={}, siteId={}, newThreshold=[{}, {}]C",
        event.equipmentId(), event.siteId(), event.minCelsius(), event.maxCelsius());
  }
}