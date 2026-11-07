package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

/**
 * Query to obtain a temporary download link for one of the caller's reports.
 *
 * @param reportId identifier of the report; required
 * @param username username of the authenticated caller, who must be the report's requester
 */
public record GetReportDownloadLinkQuery(Long reportId, String username) {
}
