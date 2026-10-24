package pe.edu.upc.ice.track.platform.device.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceDecommissionedEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceDecommissionedIntegrationEvent;

/** Translates a board retirement domain event into the Device Management published language. */
@Service
public class DeviceDecommissionedEventHandler {
  private final ApplicationEventPublisher eventPublisher;

  public DeviceDecommissionedEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @EventListener
  public void on(DeviceDecommissionedEvent event) {
    eventPublisher.publishEvent(new DeviceDecommissionedIntegrationEvent(
        event.deviceId(), event.deviceUid(), event.decommissionedAt()));
  }
}
