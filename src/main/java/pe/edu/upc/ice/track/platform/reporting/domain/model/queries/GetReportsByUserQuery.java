package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

/**
 * Query to list every report the authenticated caller has requested, most recent first.
 *
 * @param username username of the authenticated caller; required
 */
public record GetReportsByUserQuery(String username) {
}
