package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

/**
 * Query to fetch a single report by its identifier.
 *
 * @param reportId identifier of the report; required
 */
public record GetReportByIdQuery(Long reportId) {
}
