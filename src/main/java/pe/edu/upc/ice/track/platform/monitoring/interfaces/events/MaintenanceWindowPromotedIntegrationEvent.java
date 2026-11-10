package pe.edu.upc.ice.track.platform.monitoring.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when a window started with the hardware button is taken over from the
 * mobile app, keeping its identity, and becomes an {@code APP} window attributed to a technician and
 * an order.
 *
 * <p>Service Request Management links the window to the order when it was not linked yet.</p>
 *
 * @param maintenanceWindowId identity of the window
 * @param equipmentId         the unit under maintenance
 * @param startedAt           when the window originally started
 * @param promotedAt          when the app took it over, by the gateway clock
 * @param expiresAt           the new expiration
 * @param technicianProfileId the technician now attributed, or {@code null} when unknown
 * @param serviceRequestId    the order now linked, or {@code null} when unknown
 */
public record MaintenanceWindowPromotedIntegrationEvent(
    Long maintenanceWindowId,
    Long equipmentId,
    Instant startedAt,
    Instant promotedAt,
    Instant expiresAt,
    Long technicianProfileId,
    Long serviceRequestId) {
}
