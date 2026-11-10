package pe.edu.upc.ice.track.platform.monitoring.infrastructure.device.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.device.interfaces.events.DeviceStatusChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.DeviceStatusChangedStreamResource;

/**
 * Inbound infrastructure adapter that pushes board lifecycle changes of the Device Management context
 * to the owner's event stream, so the device list updates when a board is paired, rotated, revoked or
 * unpaired from another session.
 *
 * <p>{@code last_seen_at} changes are not pushed: they move with every batch, which
 * {@code telemetry-update} already announces. Runs on the {@code streamTaskExecutor} pool, after the
 * change committed, and writes only to the stream of the owner named by the event.</p>
 */
@Component
@Slf4j
public class DeviceStreamEventListener {

  /** SSE event name of a board lifecycle change. */
  public static final String DEVICE_STATUS_CHANGED_EVENT = "device-status-changed";

  private final TelemetryStreamRegistry telemetryStreamRegistry;

  public DeviceStreamEventListener(TelemetryStreamRegistry telemetryStreamRegistry) {
    this.telemetryStreamRegistry = telemetryStreamRegistry;
  }

  /**
   * @param event the board lifecycle change just stored
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(DeviceStatusChangedIntegrationEvent event) {
    if (!telemetryStreamRegistry.isFollowed(event.ownerProfileId())) {
      return;
    }
    try {
      telemetryStreamRegistry.publish(event.ownerProfileId(), DEVICE_STATUS_CHANGED_EVENT,
          new DeviceStatusChangedStreamResource(
              event.deviceId(), event.pairedEquipmentId(), event.previousStatus(), event.status(), event.changedAt()));
    } catch (RuntimeException e) {
      log.warn("Could not stream the status change of device {}: {}", event.deviceId(), e.getMessage(), e);
    }
  }
}
