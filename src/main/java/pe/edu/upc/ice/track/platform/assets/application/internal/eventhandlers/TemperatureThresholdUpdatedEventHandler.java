package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.TemperatureThresholdUpdatedEvent;
import pe.edu.upc.ice.track.platform.assets.interfaces.events.TemperatureThresholdUpdatedIntegrationEvent;

/**
 * Internal application-layer handler for the {@link TemperatureThresholdUpdatedEvent} domain
 * event.
 *
 * <p>Translates the internal domain event into a
 * {@link TemperatureThresholdUpdatedIntegrationEvent} and re-publishes it on the Spring event bus
 * so Monitoring and Alerting can pick the new band up without this context pushing it to them.</p>
 */
@Service
@Slf4j
public class TemperatureThresholdUpdatedEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher Spring application event publisher
   */
  public TemperatureThresholdUpdatedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link TemperatureThresholdUpdatedEvent} and publishes the corresponding
   * {@link TemperatureThresholdUpdatedIntegrationEvent}.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(TemperatureThresholdUpdatedEvent event) {
    log.info("Temperature threshold updated: equipmentId={}, siteId={}, newThreshold=[{}, {}]C",
        event.equipmentId(), event.siteId(), event.minCelsius(), event.maxCelsius());
    eventPublisher.publishEvent(new TemperatureThresholdUpdatedIntegrationEvent(
        event.equipmentId(), event.siteId(), event.minCelsius(), event.maxCelsius(), event.changedAt()));
  }
}