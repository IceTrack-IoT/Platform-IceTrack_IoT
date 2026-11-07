package pe.edu.upc.ice.track.platform.monitoring.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when a maintenance window is closed.
 *
 * <p>Assets Management returns the unit to the status it held before the window, Service Request
 * Management completes the window's link, and Notifications tells the owner. Published
 * synchronously, inside the transaction that closed the window.</p>
 *
 * @param maintenanceWindowId identity of the window
 * @param equipmentId         the unit that was under maintenance
 * @param origin              the origin the window ended with: {@code APP} or {@code BUTTON}
 * @param startedAt           when the window started
 * @param endedAt             when it ended
 * @param closureReason       why it ended: {@code MANUAL_APP}, {@code MANUAL_BUTTON},
 *                            {@code INTERVENTION_COMPLETED}, {@code EXPIRED} or {@code SAFETY_LIMIT}
 * @param technicianProfileId the technician attributed, or {@code null} when unknown
 * @param serviceRequestId    the order it was linked to, or {@code null} when unlinked
 */
public record MaintenanceWindowClosedIntegrationEvent(
    Long maintenanceWindowId,
    Long equipmentId,
    String origin,
    Instant startedAt,
    Instant endedAt,
    String closureReason,
    Long technicianProfileId,
    Long serviceRequestId) {
}
