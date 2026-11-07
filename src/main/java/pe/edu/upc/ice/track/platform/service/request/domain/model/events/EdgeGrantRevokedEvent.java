package pe.edu.upc.ice.track.platform.service.request.domain.model.events;

/**
 * Domain event registered when an edge grant is revoked.
 *
 * @param serviceRequestId    the request the grant was issued for
 * @param technicianProfileId the technician it was issued to
 * @param equipmentId         the unit whose gateway it opened
 * @param reason              why it was revoked, such as {@code REASSIGNED} or {@code COMPLETED}
 */
public record EdgeGrantRevokedEvent(Long serviceRequestId, Long technicianProfileId, Long equipmentId, String reason) {
}
