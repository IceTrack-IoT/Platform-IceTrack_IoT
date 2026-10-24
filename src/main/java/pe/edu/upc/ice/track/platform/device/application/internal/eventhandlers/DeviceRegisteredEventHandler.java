package pe.edu.upc.ice.track.platform.device.application.internal.eventhandlers;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceRegisteredEvent;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceRegisteredIntegrationEvent;

/** Translates the Device Management registration domain event into its published language. */
@Service
public class DeviceRegisteredEventHandler {
  private final ApplicationEventPublisher eventPublisher;

  public DeviceRegisteredEventHandler(ApplicationEventPublisher eventPublisher) {
    this.eventPublisher = eventPublisher;
  }

  @EventListener
  public void on(DeviceRegisteredEvent event) {
    eventPublisher.publishEvent(new DeviceRegisteredIntegrationEvent(
        event.deviceId(), event.deviceUid(), event.registeredAt()));
  }
}
