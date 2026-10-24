package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

/**
 * Query to list every report a user has requested, most recent first.
 *
 * @param userId identifier of the requesting user; required
 */
public record GetReportsByUserQuery(Long userId) {
}
