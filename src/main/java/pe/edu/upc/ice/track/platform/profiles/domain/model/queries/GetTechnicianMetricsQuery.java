package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

/**
 * Query to retrieve the projected operational statistics of a technician.
 *
 * @param technicianId the technician profile identifier
 */
public record GetTechnicianMetricsQuery(Long technicianId) {
}
