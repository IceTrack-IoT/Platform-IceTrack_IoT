package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent;
import pe.edu.upc.ice.track.platform.assets.interfaces.events.EquipmentRegisteredIntegrationEvent;

/**
 * Internal application-layer handler for the {@link EquipmentRegisteredEvent} domain event.
 *
 * <p>Translates the internal domain event into an {@link EquipmentRegisteredIntegrationEvent} and
 * re-publishes it on the Spring event bus for cross-context consumers.</p>
 */
@Service
@Slf4j
public class EquipmentRegisteredEventHandler {

  private final ApplicationEventPublisher eventPublisher;

  /**
   * Constructor.
   *
   * @param eventPublisher Spring application event publisher
   */
  public EquipmentRegisteredEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  /**
   * Receives the internal {@link EquipmentRegisteredEvent} and publishes the corresponding
   * {@link EquipmentRegisteredIntegrationEvent}.
   *
   * @param event the internal domain event
   */
  @EventListener
  public void on(EquipmentRegisteredEvent event) {
    log.info("Equipment registered: id={}, siteId={}, uid='{}', type={}, threshold=[{}, {}]C",
        event.equipmentId(), event.siteId(), event.uid(), event.equipmentType(),
        event.minCelsius(), event.maxCelsius());
    eventPublisher.publishEvent(new EquipmentRegisteredIntegrationEvent(
        event.equipmentId(),
        event.siteId(),
        event.uid(),
        event.equipmentType() == null ? null : event.equipmentType().name(),
        event.minCelsius(),
        event.maxCelsius(),
        event.registeredAt()));
  }
}