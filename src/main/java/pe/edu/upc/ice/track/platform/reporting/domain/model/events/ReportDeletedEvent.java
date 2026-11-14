package pe.edu.upc.ice.track.platform.reporting.domain.model.events;

/**
 * Published when a report has been deleted, so its stored file can be removed.
 *
 * <p>It is handled only after the transaction that deleted the report has committed: the file is
 * never removed for a report that is still there.</p>
 *
 * @param reportId identifier of the deleted report
 * @param fileKey  storage key of its file, or {@code null} when it had none (a failed report)
 */
public record ReportDeletedEvent(Long reportId, String fileKey) {
}
