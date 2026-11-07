package pe.edu.upc.ice.track.platform.device.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceUnpairedEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceUnpairedIntegrationEvent;

/** Translates an unpairing domain event into the event other bounded contexts may consume. */
@Service
public class DeviceUnpairedEventHandler {
  private final ApplicationEventPublisher eventPublisher;

  public DeviceUnpairedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @EventListener
  public void on(DeviceUnpairedEvent event) {
    eventPublisher.publishEvent(new DeviceUnpairedIntegrationEvent(
        event.deviceId(), event.equipmentId(), event.unpairedAt()));
  }
}
