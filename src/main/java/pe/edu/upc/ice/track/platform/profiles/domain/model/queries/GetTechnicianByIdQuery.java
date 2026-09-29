package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

/**
 * Query to retrieve a technician by its identifier.
 *
 * @param technicianId the technician identifier
 */
public record GetTechnicianByIdQuery(Long technicianId) {
}
