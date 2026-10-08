package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

/**
 * Query to retrieve a dashboard configuration by its identifier.
 *
 * @param dashboardConfigId the configuration identifier
 */
public record GetDashboardConfigByIdQuery(Long dashboardConfigId) {
}
