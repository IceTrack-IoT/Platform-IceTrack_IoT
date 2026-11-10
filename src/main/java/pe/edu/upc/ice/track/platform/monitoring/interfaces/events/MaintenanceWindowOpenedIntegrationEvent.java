package pe.edu.upc.ice.track.platform.monitoring.interfaces.events;

import java.time.Instant;

/**
 * Integration event published when a maintenance window is opened on a unit.
 *
 * <p>The published language of the {@code monitoring} context for this fact. Assets Management moves
 * the unit to {@code MAINTENANCE}, Service Request Management links the window to its order, and
 * Notifications tells the owner. Published synchronously, inside the transaction that stored the
 * window.</p>
 *
 * @param maintenanceWindowId identity of the window
 * @param equipmentId         the unit under maintenance
 * @param origin              how it was started: {@code APP} or {@code BUTTON}
 * @param startedAt           when the gateway started it, by its clock
 * @param expiresAt           when it expires unless closed earlier
 * @param technicianProfileId the technician working on the unit, or {@code null} when unknown
 * @param serviceRequestId    the order the window belongs to, or {@code null} when unlinked
 */
public record MaintenanceWindowOpenedIntegrationEvent(
    Long maintenanceWindowId,
    Long equipmentId,
    String origin,
    Instant startedAt,
    Instant expiresAt,
    Long technicianProfileId,
    Long serviceRequestId) {

  /**
   * @return {@code true} when no order is linked to the window
   */
  public boolean unlinked() {
    return serviceRequestId == null;
  }
}
