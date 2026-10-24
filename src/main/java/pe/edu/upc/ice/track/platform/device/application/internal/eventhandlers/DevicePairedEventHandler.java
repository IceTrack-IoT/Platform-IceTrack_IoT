package pe.edu.upc.ice.track.platform.device.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DevicePairedEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DevicePairedIntegrationEvent;

/** Translates a pairing domain event into the event other bounded contexts may consume. */
@Service
public class DevicePairedEventHandler {
  private final ApplicationEventPublisher eventPublisher;

  public DevicePairedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @EventListener
  public void on(DevicePairedEvent event) {
    eventPublisher.publishEvent(new DevicePairedIntegrationEvent(
        event.deviceId(), event.equipmentId(), event.pairedAt()));
  }
}
