package pe.edu.upc.ice.track.platform.monitoring.domain.model.events;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;

import java.time.Instant;

/**
 * Domain event registered when a window started with the hardware button is taken over from the
 * mobile app by a technician, keeping its identity.
 *
 * @param maintenanceWindowId identity of the window
 * @param equipmentId         the unit under maintenance
 * @param startedAt           when the window originally started
 * @param promotedAt          when the app took it over, by the gateway clock
 * @param expiresAt           the new expiration
 * @param technicianProfileId the technician now attributed, or {@code null} when unknown
 * @param serviceRequestId    the order now linked, or {@code null} when unknown
 */
public record MaintenanceWindowPromotedEvent(
    Long maintenanceWindowId,
    Long equipmentId,
    Instant startedAt,
    Instant promotedAt,
    Instant expiresAt,
    Long technicianProfileId,
    Long serviceRequestId) {

  /**
   * @param window     the promoted window
   * @param promotedAt when the app took it over
   * @return the populated event
   */
  public static MaintenanceWindowPromotedEvent from(MaintenanceWindow window, Instant promotedAt) {
    return new MaintenanceWindowPromotedEvent(
        window.getId(), window.getEquipmentId(), window.getStartedAt(), promotedAt, window.getExpiresAt(),
        window.getTechnicianProfileId(), window.getServiceRequestId());
  }
}
