package pe.edu.upc.ice.track.platform.device.application.internal.eventhandlers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceStatusChangedEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceStatusChangedIntegrationEvent;

/**
 * Translates a board's lifecycle change into the event other bounded contexts may consume, naming
 * the owner it concerns: the owner of the unit the board is, or was until the change, linked to. A
 * change whose unit has no owner any more concerns nobody and is not published.
 */
@Service
@Slf4j
public class DeviceStatusChangedEventHandler {

  private final ApplicationEventPublisher eventPublisher;
  private final ExternalAssetManagementServiceForDevice externalAssetManagementService;

  public DeviceStatusChangedEventHandler(
      ApplicationEventPublisher eventPublisher,
      ExternalAssetManagementServiceForDevice externalAssetManagementService) {
    this.eventPublisher = eventPublisher;
    this.externalAssetManagementService = externalAssetManagementService;
  }

  @EventListener
  public void on(DeviceStatusChangedEvent event) {
    var ownerProfileId = externalAssetManagementService.fetchOwnerId(event.equipmentId());
    if (ownerProfileId.isEmpty()) {
      log.debug("Status change of device {} not published: no owner for equipment {}",
          event.deviceId(), event.equipmentId());
      return;
    }
    eventPublisher.publishEvent(new DeviceStatusChangedIntegrationEvent(
        event.deviceId(),
        ownerProfileId.get(),
        event.pairedEquipmentId(),
        event.previousStatus().name(),
        event.status().name(),
        event.changedAt()));
  }
}
