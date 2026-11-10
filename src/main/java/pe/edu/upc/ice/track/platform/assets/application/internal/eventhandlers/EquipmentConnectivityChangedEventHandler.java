package pe.edu.upc.ice.track.platform.assets.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Site;
import pe.edu.upc.ice.track.platform.assets.domain.model.events.EquipmentConnectivityChangedEvent;
import pe.edu.upc.ice.track.platform.assets.domain.repositories.SiteRepository;
import pe.edu.upc.ice.track.platform.assets.interfaces.events.EquipmentConnectivityChangedIntegrationEvent;

/**
 * Translates a unit's connectivity change into the event other bounded contexts may consume, naming
 * the owner of the unit's site. A unit whose site no longer exists concerns nobody and is not
 * published.
 */
@Service
@Slf4j
public class EquipmentConnectivityChangedEventHandler {

  private final ApplicationEventPublisher eventPublisher;
  private final SiteRepository siteRepository;

  public EquipmentConnectivityChangedEventHandler(ApplicationEventPublisher eventPublisher, SiteRepository siteRepository) {
    this.eventPublisher = eventPublisher;
    this.siteRepository = siteRepository;
  }

  @EventListener
  public void on(EquipmentConnectivityChangedEvent event) {
    var ownerProfileId = siteRepository.findById(event.siteId()).map(Site::getOwnerId);
    if (ownerProfileId.isEmpty()) {
      log.debug("Connectivity change of equipment {} not published: site {} not found",
          event.equipmentId(), event.siteId());
      return;
    }
    eventPublisher.publishEvent(new EquipmentConnectivityChangedIntegrationEvent(
        event.equipmentId(),
        ownerProfileId.get(),
        event.previousState().name(),
        event.state().name(),
        event.lastReceivedAt(),
        event.underMaintenance(),
        event.changedAt()));
  }
}
