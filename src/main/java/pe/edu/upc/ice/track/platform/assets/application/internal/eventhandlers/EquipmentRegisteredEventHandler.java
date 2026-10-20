package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent;

/**
 * Internal application-layer handler for the {@link EquipmentRegisteredEvent} domain event.
 */
@Service
@Slf4j
public class EquipmentRegisteredEventHandler {

  /**
   * Receives the {@link EquipmentRegisteredEvent} once the new unit has been persisted.
   *
   * @param event the domain event
   */
  @EventListener
  public void on(EquipmentRegisteredEvent event) {
    log.info("Equipment registered: id={}, siteId={}, uid='{}', type={}, threshold=[{}, {}]C",
        event.equipmentId(), event.siteId(), event.uid(), event.equipmentType(),
        event.minCelsius(), event.maxCelsius());
  }
}
