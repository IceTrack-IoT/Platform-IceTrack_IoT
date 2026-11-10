package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent;

/**
 * Internal application-layer handler for the {@link TemperatureThresholdUpdatedEvent} domain
 * event.
 */
@Service
@Slf4j
public class TemperatureThresholdUpdatedEventHandler {

  /**
   * Receives the {@link TemperatureThresholdUpdatedEvent} once the new band has been persisted.
   *
   * @param event the domain event
   */
  @EventListener
  public void on(TemperatureThresholdUpdatedEvent event) {
    log.info("Temperature threshold updated: equipmentId={}, siteId={}, newThreshold=[{}, {}]C",
        event.equipmentId(), event.siteId(), event.minCelsius(), event.maxCelsius());
  }
}
