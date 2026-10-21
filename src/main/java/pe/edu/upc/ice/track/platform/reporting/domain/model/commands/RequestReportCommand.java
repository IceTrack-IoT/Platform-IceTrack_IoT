package pe.edu.upc.ice.track.platform.reporting.domain.model.commands;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.time.LocalDateTime;

/**
 * Command to request a new report.
 *
 * <p>{@code siteId}, {@code equipmentId} and {@code technicianId} are each optional: which ones
 * are required depends on {@code type} alone, and is enforced by that type's
 * {@code ReportGenerationStrategy} rather than by this command.</p>
 *
 * @param title        display title for the report; required
 * @param type         which {@code ReportGenerationStrategy} computes it; required
 * @param format       the export format the requester wants; required
 * @param userId       identifier of the user requesting the report; required
 * @param siteId       restrict to a single site, may be {@code null}
 * @param equipmentId  restrict to a single unit, may be {@code null}
 * @param technicianId restrict to a single technician, may be {@code null}
 * @param from         inclusive start of the reporting window; required
 * @param to           inclusive end of the reporting window; required
 */
public record RequestReportCommand(
    String title,
    ReportType type,
    ReportFormat format,
    Long userId,
    Long siteId,
    Long equipmentId,
    Long technicianId,
    LocalDateTime from,
    LocalDateTime to) {
}
