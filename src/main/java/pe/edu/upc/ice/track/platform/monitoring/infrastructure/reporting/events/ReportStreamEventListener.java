package pe.edu.upc.ice.track.platform.monitoring.infrastructure.reporting.events;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ReportStatusChangedStreamResource;
import pe.edu.upc.ice.track.platform.reporting.interfaces.events.ReportStatusChangedIntegrationEvent;

/**
 * Inbound infrastructure adapter that pushes report status changes of the Reporting context to the
 * owner's event stream, so a report being watched needs no polling.
 *
 * <p>Runs on the {@code streamTaskExecutor} pool, after the change is stored. Only the stream of the
 * owner named by the event is written to. Failures are logged and swallowed.</p>
 */
@Component
@Slf4j
public class ReportStreamEventListener {

  /** SSE event name of a report status change. */
  public static final String REPORT_STATUS_CHANGED_EVENT = "report-status-changed";

  private final TelemetryStreamRegistry telemetryStreamRegistry;

  public ReportStreamEventListener(TelemetryStreamRegistry telemetryStreamRegistry) {
    this.telemetryStreamRegistry = telemetryStreamRegistry;
  }

  /**
   * @param event the report status change just stored
   */
  @Async("streamTaskExecutor")
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
  public void on(ReportStatusChangedIntegrationEvent event) {
    if (!telemetryStreamRegistry.isFollowed(event.ownerProfileId())) {
      return;
    }
    try {
      telemetryStreamRegistry.publish(event.ownerProfileId(), REPORT_STATUS_CHANGED_EVENT,
          new ReportStatusChangedStreamResource(
              event.reportId(), event.previousStatus(), event.status(), event.changedAt()));
    } catch (RuntimeException e) {
      log.warn("Could not stream the status change of report {}: {}", event.reportId(), e.getMessage(), e);
    }
  }
}
