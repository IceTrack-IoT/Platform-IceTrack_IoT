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
 * <p>{@code serviceRequestId} is required for a {@code SERVICE_INTERVENTION} report and refused for
 * every other type; the request must be the caller's and completed. Such a report may leave
 * {@code from} and {@code to} out, and then covers the request's own period.</p>
 *
 * @param title            display title for the report; required
 * @param type             which {@code ReportGenerationStrategy} computes it; required
 * @param format           the export format the requester wants; required
 * @param username         username of the authenticated caller, resolved by the command service to
 *                         the account the report belongs to and the owner whose assets it covers;
 *                         required
 * @param siteId           restrict to a single site, may be {@code null}
 * @param equipmentId      restrict to a single unit, may be {@code null}
 * @param technicianId     restrict to a single technician, may be {@code null}
 * @param from             inclusive start of the reporting window; required except for a service document
 * @param to               inclusive end of the reporting window; required except for a service document
 * @param serviceRequestId the service request a service document is about, may be {@code null}
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
    Instant to,
    Long serviceRequestId) {

  /**
   * Creates the command without a service request.
   */
  public RequestReportCommand(String title, ReportType type, ReportFormat format, String username, Long siteId,
                              Long equipmentId, Long technicianId, Instant from, Instant to) {
    this(title, type, format, username, siteId, equipmentId, technicianId, from, to, null);
  }
}
