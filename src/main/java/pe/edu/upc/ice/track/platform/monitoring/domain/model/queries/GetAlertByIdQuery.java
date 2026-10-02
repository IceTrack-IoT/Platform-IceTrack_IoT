package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

/**
 * Query to fetch a single alert by identifier.
 *
 * @param alertId identifier of the alert
 */
public record GetAlertByIdQuery(Long alertId) {
}
