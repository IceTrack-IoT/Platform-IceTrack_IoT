package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

/**
 * Query to fetch one of the caller's reports by its identifier.
 *
 * @param reportId identifier of the report; required
 * @param username username of the authenticated caller; a report requested by anybody else is
 *                 answered as absent
 */
public record GetReportByIdQuery(Long reportId, String username) {
}
