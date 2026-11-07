package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

/**
 * Domain event registered when an edge grant is issued to the technician of a request.
 *
 * @param serviceRequestId    the request the grant was issued for
 * @param technicianProfileId the technician it was issued to
 * @param equipmentId         the unit whose gateway it opens
 */
public record EdgeGrantIssuedEvent(Long serviceRequestId, Long technicianProfileId, Long equipmentId) {
}
