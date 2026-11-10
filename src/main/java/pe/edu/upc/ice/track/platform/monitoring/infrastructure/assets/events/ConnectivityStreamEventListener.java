package pe.edu.upc.ice.track.platform.monitoring.infrastructure.assets.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.assets.interfaces.events.EquipmentConnectivityChangedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalDeviceServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ConnectivityChangedStreamResource;

/**
 * Inbound infrastructure adapter that pushes the connectivity changes of the owner's units to the
 * owner's event stream: offline detection found a unit silent, its next batch brought it back, it was
 * deactivated or reactivated, or a maintenance window opened or closed on it. Open screens then
 * follow the state without a clock tick of their own.
 *
 * <p>Runs on the {@code streamTaskExecutor} pool, after the change committed, so the board linked to
 * the unit is looked up off the request thread. Only the stream of the owner named by the event is
 * written to. Failures are logged and swallowed.</p>
 */
@Component
@Slf4j
public class ConnectivityStreamEventListener {

  /** SSE event name of a connectivity change. */
  public static final String CONNECTIVITY_CHANGED_EVENT = "connectivity-changed";

  private final TelemetryStreamRegistry telemetryStreamRegistry;
  private final ExternalDeviceServiceForMonitoring externalDeviceService;

  public ConnectivityStreamEventListener(
      TelemetryStreamRegistry telemetryStreamRegistry, ExternalDeviceServiceForMonitoring externalDeviceService) {
    this.telemetryStreamRegistry = telemetryStreamRegistry;
    this.externalDeviceService = externalDeviceService;
  }

  /**
   * @param event the connectivity change just stored
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(EquipmentConnectivityChangedIntegrationEvent event) {
    if (!telemetryStreamRegistry.isFollowed(event.ownerProfileId())) {
      return;
    }
    try {
      var deviceId = externalDeviceService.fetchLinkedDeviceId(event.equipmentId()).orElse(null);
      telemetryStreamRegistry.publish(event.ownerProfileId(), CONNECTIVITY_CHANGED_EVENT,
          new ConnectivityChangedStreamResource(
              event.equipmentId(),
              deviceId,
              event.previousState(),
              event.state(),
              event.lastReceivedAt(),
              event.underMaintenance(),
              event.changedAt()));
    } catch (RuntimeException e) {
      log.warn("Could not stream the connectivity change of equipment {}: {}", event.equipmentId(), e.getMessage(), e);
    }
  }
}
