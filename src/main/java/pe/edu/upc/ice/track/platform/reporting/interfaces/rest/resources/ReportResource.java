package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

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
public record ReportResource(
    Long id,
    String title,
    String type,
    String format,
    String status,
    String resultPayload,
    Long userId,
    Long siteId,
    Long equipmentId,
    Long technicianId,
    LocalDateTime from,
    LocalDateTime to,
    LocalDateTime requestedAt,
    LocalDate completedAt) {
}
