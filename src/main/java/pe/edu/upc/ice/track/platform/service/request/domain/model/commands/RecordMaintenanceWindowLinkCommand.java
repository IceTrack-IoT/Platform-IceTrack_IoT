package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;

/**
 * Command recording, on a service request, a maintenance window Monitoring and Alerting opened on its
 * unit.
 *
 * <p>Issued by the inbound adapter reacting to the maintenance window integration events, never by a
 * REST caller, so it carries no username.</p>
 *
 * @param serviceRequestId    the request the window is attributed to; required
 * @param windowId            identity of the window; required
 * @param equipmentId         the unit the window was opened on, which must be the request's; required
 * @param origin              how the window was started; required
 * @param technicianProfileId the technician attributed to the window, may be {@code null}
 * @param windowStartedAt     when the window started; required
 * @param attributedAt        when the window was attributed to the request: its start, or when the app
 *                            took over a window started with the button; required
 */
public record RecordMaintenanceWindowLinkCommand(
    Long serviceRequestId,
    Long windowId,
    Long equipmentId,
    MaintenanceOrigin origin,
    Long technicianProfileId,
    Instant windowStartedAt,
    Instant attributedAt) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public RecordMaintenanceWindowLinkCommand {
    if (serviceRequestId == null || windowId == null || equipmentId == null || origin == null
        || windowStartedAt == null || attributedAt == null) {
      throw new IllegalArgumentException(
          "serviceRequestId, windowId, equipmentId, origin, windowStartedAt and attributedAt are required");
    }
  }
}
