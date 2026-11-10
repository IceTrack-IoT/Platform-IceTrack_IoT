package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Data of the {@code report-status-changed} stream event: a report of the stream owner moved to
 * {@code GENERATING}, {@code COMPLETED} or {@code FAILED}.
 */
@Schema(
    name = "ReportStatusChangedStreamEvent",
    description = "Data of the report-status-changed event",
    example = "{\"report_id\": 8, \"previous_status\": \"GENERATING\", \"status\": \"COMPLETED\", "
        + "\"changed_at\": \"2026-11-09T10:02:09Z\"}")
public record ReportStatusChangedStreamResource(
    @Schema(description = "Identifier of the report", example = "8")
    Long reportId,

    @Schema(description = "Status before the change (ReportStatus)", example = "GENERATING")
    String previousStatus,

    @Schema(description = "Status after the change (ReportStatus)", example = "COMPLETED")
    String status,

    @Schema(description = "When the change happened", example = "2026-11-09T10:02:09Z")
    Instant changedAt
) {
}
