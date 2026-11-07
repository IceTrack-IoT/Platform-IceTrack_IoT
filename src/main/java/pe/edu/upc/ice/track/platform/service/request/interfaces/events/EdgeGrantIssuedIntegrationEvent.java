package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

/**
 * Integration event published when an edge grant is issued to the technician of a request.
 *
 * <p>The published language of the {@code service.request} context for this fact. Assets
 * Management subscribes to it to increment the unit's edge configuration version, so the gateway
 * learns the new grant on its next poll.</p>
 *
 * @param serviceRequestId    the request the grant was issued for
 * @param technicianProfileId the technician it was issued to
 * @param equipmentId         the unit whose gateway it opens
 */
public record EdgeGrantIssuedIntegrationEvent(Long serviceRequestId, Long technicianProfileId, Long equipmentId) {
}
