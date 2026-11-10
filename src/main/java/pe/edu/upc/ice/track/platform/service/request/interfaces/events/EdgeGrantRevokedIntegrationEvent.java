package pe.edu.upc.ice.track.platform.service.request.interfaces.events;

/**
 * Integration event published when an edge grant is revoked, because its request was reassigned,
 * canceled or completed, or because the technician regenerated it.
 *
 * <p>The published language of the {@code service.request} context for this fact. Assets
 * Management subscribes to it to increment the unit's edge configuration version, so the gateway
 * drops the grant on its next poll.</p>
 *
 * @param serviceRequestId    the request the grant was issued for
 * @param technicianProfileId the technician it was issued to
 * @param equipmentId         the unit whose gateway it opened
 * @param reason              why it was revoked
 */
public record EdgeGrantRevokedIntegrationEvent(
    Long serviceRequestId, Long technicianProfileId, Long equipmentId, String reason) {
}
