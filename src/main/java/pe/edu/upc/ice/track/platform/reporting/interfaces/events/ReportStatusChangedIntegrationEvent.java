package pe.edu.upc.ice.track.platform.reporting.interfaces.events;

import java.time.Instant;

/**
 * Integration event published after a report moved to {@code GENERATING}, {@code COMPLETED} or
 * {@code FAILED}, once the change is stored.
 *
 * @param reportId       the report
 * @param ownerProfileId the owner profile of the account that requested the report
 * @param previousStatus the status before the change
 * @param status         the status after the change
 * @param changedAt      when the change happened
 */
public record ReportStatusChangedIntegrationEvent(
    Long reportId,
    Long ownerProfileId,
    String previousStatus,
    String status,
    Instant changedAt) {
}
