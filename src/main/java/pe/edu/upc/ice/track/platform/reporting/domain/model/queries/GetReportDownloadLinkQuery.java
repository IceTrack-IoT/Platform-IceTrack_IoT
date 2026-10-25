package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

/**
 * Query to obtain a temporary download link for a report's file.
 *
 * @param reportId identifier of the report; required
 * @param userId   identifier of the user asking; must be the report's requester
 */
public record GetReportDownloadLinkQuery(Long reportId, Long userId) {
}
