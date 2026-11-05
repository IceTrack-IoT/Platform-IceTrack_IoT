package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Resource representing a report, as returned to API consumers.
 *
 * @param id             identifier of the report
 * @param title          display title for the report
 * @param type           the report type's enum name
 * @param format         the output format's enum name
 * @param status         the report's current status, one of {@code PENDING}, {@code GENERATING},
 *                       {@code COMPLETED}, {@code FAILED}
 * @param resultPayload  a JSON array of the report's KPIs once {@code COMPLETED}, or the failure
 *                       reason once {@code FAILED}; {@code null} while still {@code PENDING}/
 *                       {@code GENERATING}
 * @param userId         identifier of the user who requested the report
 * @param siteId         identifier of the site the report was scoped to, nullable
 * @param equipmentId    identifier of the unit the report was scoped to, nullable
 * @param technicianId   identifier of the technician the report was scoped to, nullable
 * @param from           inclusive start of the date range the report covers
 * @param to             inclusive end of the date range the report covers
 * @param requestedAt    when the report was requested
 * @param completedAt    the date the report reached {@code COMPLETED} or {@code FAILED},
 *                       {@code null} while still in progress
 */
@Schema(
    name = "ReportResponse",
    description = "Report requested by a platform user, with its generation status",
    example = "{\"id\": 8, \"title\": \"Equipment uptime - October\", \"type\": \"EQUIPMENT_UPTIME\", "
        + "\"format\": \"PDF\", \"status\": \"COMPLETED\", \"result_payload\": \"[{\\\"kpi\\\": "
        + "\\\"uptime_percentage\\\", \\\"value\\\": 99.2}]\", \"user_id\": 42, \"site_id\": 1, "
        + "\"equipment_id\": null, \"technician_id\": null, \"from\": \"2026-10-01T00:00:00\", "
        + "\"to\": \"2026-10-31T23:59:59\", \"requested_at\": \"2026-11-01T08:00:00\", "
        + "\"completed_at\": \"2026-11-01\"}"
)
public record ReportResource(
    @Schema(description = "Report unique identifier", example = "8")
    Long id,

    @Schema(description = "Display title for the report", example = "Equipment uptime - October")
    String title,

    @Schema(description = "Report type's enum name", example = "EQUIPMENT_UPTIME",
        allowableValues = {"MAINTENANCE_COMPLIANCE", "EQUIPMENT_UPTIME", "TECHNICIAN_PERFORMANCE",
            "TEMPERATURE_EXCURSION"})
    String type,

    @Schema(description = "Output format's enum name", example = "PDF",
        allowableValues = {"PDF", "EXCEL", "CSV"})
    String format,

    @Schema(description = "Current generation status", example = "COMPLETED",
        allowableValues = {"PENDING", "GENERATING", "COMPLETED", "FAILED"})
    String status,

    @Schema(description = "JSON array of the report's KPIs once completed, or the failure reason once "
        + "failed; null while pending or generating",
        example = "[{\"kpi\": \"uptime_percentage\", \"value\": 99.2}]", nullable = true)
    String resultPayload,

    @Schema(description = "Identifier of the user who requested the report", example = "42")
    Long userId,

    @Schema(description = "Identifier of the site the report was scoped to", example = "1",
        nullable = true)
    Long siteId,

    @Schema(description = "Identifier of the unit the report was scoped to", nullable = true)
    Long equipmentId,

    @Schema(description = "Identifier of the technician the report was scoped to", nullable = true)
    Long technicianId,

    @Schema(description = "Inclusive start of the date range the report covers",
        example = "2026-10-01T00:00:00")
    LocalDateTime from,

    @Schema(description = "Inclusive end of the date range the report covers",
        example = "2026-10-31T23:59:59")
    LocalDateTime to,

    @Schema(description = "When the report was requested", example = "2026-11-01T08:00:00")
    LocalDateTime requestedAt,

    @Schema(description = "Date the report reached COMPLETED or FAILED; null while in progress",
        example = "2026-11-01", nullable = true)
    LocalDate completedAt) {
}
