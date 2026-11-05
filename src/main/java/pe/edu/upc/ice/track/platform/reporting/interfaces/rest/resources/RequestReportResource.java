package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * Resource carrying the data needed to request a new report.
 *
 * @param title        display title for the report
 * @param type         the report type, by its enum name (e.g. {@code "EQUIPMENT_UPTIME"})
 * @param format       the intended output format, by its enum name (e.g. {@code "PDF"})
 * @param userId       identifier of the requesting user
 * @param siteId       identifier of the site to scope the report to, nullable
 * @param equipmentId  identifier of a single unit to scope the report to, nullable
 * @param technicianId identifier of the technician to scope a performance report to, nullable
 * @param from         inclusive start of the date range
 * @param to           inclusive end of the date range
 */
@Schema(
    name = "RequestReportRequest",
    description = "Request to generate a new report",
    example = "{\"title\": \"Equipment uptime - October\", \"type\": \"EQUIPMENT_UPTIME\", "
        + "\"format\": \"PDF\", \"user_id\": 42, \"site_id\": 1, \"equipment_id\": null, "
        + "\"technician_id\": null, \"from\": \"2026-10-01T00:00:00\", \"to\": \"2026-10-31T23:59:59\"}"
)
public record RequestReportResource(
    @Schema(description = "Display title for the report", example = "Equipment uptime - October")
    String title,

    @Schema(description = "Report type, by its enum name", example = "EQUIPMENT_UPTIME",
        allowableValues = {"MAINTENANCE_COMPLIANCE", "EQUIPMENT_UPTIME", "TECHNICIAN_PERFORMANCE",
            "TEMPERATURE_EXCURSION"})
    String type,

    @Schema(description = "Intended output format, by its enum name", example = "PDF",
        allowableValues = {"PDF", "EXCEL", "CSV"})
    String format,

    @Schema(description = "Identifier of the requesting user", example = "42")
    Long userId,

    @Schema(description = "Identifier of the site to scope the report to", example = "1",
        nullable = true)
    Long siteId,

    @Schema(description = "Identifier of a single unit to scope the report to", nullable = true)
    Long equipmentId,

    @Schema(description = "Identifier of the technician to scope a performance report to",
        nullable = true)
    Long technicianId,

    @Schema(description = "Inclusive start of the date range", example = "2026-10-01T00:00:00")
    LocalDateTime from,

    @Schema(description = "Inclusive end of the date range", example = "2026-10-31T23:59:59")
    LocalDateTime to) {
}
