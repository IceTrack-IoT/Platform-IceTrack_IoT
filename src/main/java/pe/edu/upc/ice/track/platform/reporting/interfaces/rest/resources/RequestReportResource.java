package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

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
public record RequestReportResource(
    String title,
    String type,
    String format,
    Long userId,
    Long siteId,
    Long equipmentId,
    Long technicianId,
    LocalDateTime from,
    LocalDateTime to) {
}
