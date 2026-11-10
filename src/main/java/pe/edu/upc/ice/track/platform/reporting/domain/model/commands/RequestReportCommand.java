package pe.edu.upc.ice.track.platform.reporting.domain.model.commands;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportFormat;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportType;

import java.time.Instant;

/**
 * Command to request a new report on behalf of the authenticated caller.
 *
 * <p>{@code siteId}, {@code equipmentId} and {@code technicianId} are each optional: which ones
 * are required depends on {@code type} alone, and is enforced by that type's
 * {@code ReportGenerationStrategy} rather than by this command. A site or unit, when given, must
 * belong to the caller.</p>
 *
 * @param title        display title for the report; required
 * @param type         which {@code ReportGenerationStrategy} computes it; required
 * @param format       the export format the requester wants; required
 * @param username     username of the authenticated caller, resolved by the command service to
 *                     the account the report belongs to and the owner whose assets it covers;
 *                     required
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
    String username,
    Long siteId,
    Long equipmentId,
    Long technicianId,
    Instant from,
    Instant to) {
}
