package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentRegisteredEvent;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;

/**
 * Internal application-layer handler for the {@link EquipmentRegisteredEvent} domain event.
 *
 * <p>Keeps the equipment counter of the site the unit was installed at. It runs synchronously,
 * inside the transaction that registered the unit, so the unit and the counter are committed or
 * rolled back together and the counter never drifts from the units actually saved.</p>
 */
@Service
@Slf4j
public class EquipmentRegisteredEventHandler {

  private final SiteRepository siteRepository;

  /**
   * Constructor
   *
   * @param siteRepository the {@link SiteRepository} instance
   */
  public EquipmentRegisteredEventHandler(SiteRepository siteRepository) {
    this.siteRepository = siteRepository;
  }

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
    siteRepository.findById(event.siteId()).ifPresent(site -> {
      site.incrementEquipmentCount();
      siteRepository.save(site);
    });
  }
}
