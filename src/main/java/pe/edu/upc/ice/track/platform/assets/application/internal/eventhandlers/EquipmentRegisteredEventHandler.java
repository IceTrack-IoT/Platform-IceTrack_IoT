package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent;

/**
 * Internal application-layer handler for the {@link EquipmentRegisteredEvent} domain event.
 *
 * <p>Like its site counterpart, it observes and traces rather than translates: cataloguing a unit
 * has to reach no other context synchronously. It is registered after the owning transaction
 * commits, so a trace line can never advertise a unit whose insert was rolled back.</p>
 */
@Component
@Slf4j
public class EquipmentRegisteredEventHandler {

  /**
   * Receives the internal {@link EquipmentRegisteredEvent} after the transaction committed.
   *
   * @param event the internal domain event
   */
  @TransactionalEventListener
  public void on(EquipmentRegisteredEvent event) {
    log.info("Equipment registered: id={}, siteId={}, uid='{}', type={}, threshold=[{}, {}]C",
        event.equipmentId(), event.siteId(), event.uid(), event.equipmentType(),
        event.minCelsius(), event.maxCelsius());
  }
}