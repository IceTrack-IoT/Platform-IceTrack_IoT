package pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.assetmanagement.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertStatusChangedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.ReadingRecordedEvent;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform.TelemetryStreamResourceFromEventAssembler;

import java.util.function.Supplier;

/**
 * Pushes recorded readings and alert status changes to the telemetry streams of the unit's owner.
 *
 * <p>Runs only once the transaction that recorded the change has committed, so a rolled back batch
 * or transition is never streamed, and on the {@code streamTaskExecutor} pool, so neither the owner
 * lookup nor a slow client delays the ingestion request. Failures are logged and swallowed: the
 * change is already stored and the dashboard reloads it over the REST API.</p>
 *
 * <p><strong>Tenant isolation.</strong> The recipient is resolved from the unit itself, through the
 * Asset Management ACL (unit, then site, then owner profile), and the event is written only to the
 * streams registered under that owner. Nothing in the event decides who receives it, and a unit whose
 * owner cannot be resolved is streamed to nobody.</p>
 *
 * <p>Every event is written under the SSE event name {@value #TELEMETRY_UPDATE_EVENT}; its data
 * carries a {@code type} of {@code READING_RECORDED} or {@code ALERT_STATUS_CHANGED}. When no stream
 * is open on this instance, the owner is not even looked up.</p>
 */
@Component
@Slf4j
public class TelemetryStreamEventDispatcher {

  /** SSE event name of every reading and alert update. */
  public static final String TELEMETRY_UPDATE_EVENT = "telemetry-update";

  private final TelemetryStreamRegistry telemetryStreamRegistry;
  private final ExternalAssetManagementService externalAssetManagementService;

  public TelemetryStreamEventDispatcher(
      TelemetryStreamRegistry telemetryStreamRegistry,
      ExternalAssetManagementService externalAssetManagementService) {
    this.telemetryStreamRegistry = telemetryStreamRegistry;
    this.externalAssetManagementService = externalAssetManagementService;
  }

  /**
   * @param event the reading batch just recorded
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(ReadingRecordedEvent event) {
    dispatch(event.equipmentId(), () -> TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(event));
  }

  /**
   * @param event the alert that was raised or moved to another status
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(AlertStatusChangedEvent event) {
    dispatch(event.equipmentId(), () -> TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(event));
  }

  /**
   * Resolves the owner of a unit and writes the event to that owner's streams only.
   *
   * @param equipmentId identifier of the unit the event concerns
   * @param payload     builds the event data, only when somebody may receive it
   */
  private void dispatch(Long equipmentId, Supplier<Object> payload) {
    if (!telemetryStreamRegistry.hasSubscribers()) {
      return;
    }
    try {
      var ownerProfileId = externalAssetManagementService.fetchOwnerId(equipmentId);
      if (ownerProfileId.isEmpty()) {
        log.debug("No owner resolved for equipment {}; telemetry update not streamed", equipmentId);
        return;
      }
      if (telemetryStreamRegistry.streamCount(ownerProfileId.get()) == 0) {
        return;
      }
      telemetryStreamRegistry.publish(ownerProfileId.get(), TELEMETRY_UPDATE_EVENT, payload.get());
    } catch (RuntimeException e) {
      log.warn("Could not stream a telemetry update of equipment {}: {}", equipmentId, e.getMessage(), e);
    }
  }
}
