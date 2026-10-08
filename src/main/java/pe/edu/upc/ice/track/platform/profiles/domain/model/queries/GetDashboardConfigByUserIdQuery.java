package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

/**
 * Query to retrieve the dashboard configuration of a platform account.
 *
 * @param userId the identifier of the account
 */
public record GetDashboardConfigByUserIdQuery(Long userId) {
}
