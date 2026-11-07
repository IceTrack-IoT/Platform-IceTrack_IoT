package pe.edu.upc.ice.track.platform.reporting.domain.model.events;

/**
 * Published when a report has been saved in {@code PENDING} status, so its generation can start.
 *
 * <p>It is handled only after the transaction that saved the report has committed, so the worker
 * always finds the row.</p>
 *
 * @param reportId identifier of the pending report
 */
public record ReportRequestedEvent(Long reportId) {
}
