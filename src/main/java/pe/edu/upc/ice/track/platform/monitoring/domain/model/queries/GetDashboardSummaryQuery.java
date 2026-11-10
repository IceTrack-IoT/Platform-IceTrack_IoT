package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

/**
 * Query for the dashboard summary of an owner.
 *
 * @param username username of the authenticated caller, resolved to the owner whose figures are
 *                 computed; required
 * @param siteId   restrict every figure to a single site of the caller, may be {@code null}
 */
public record GetDashboardSummaryQuery(String username, Long siteId) {
}
