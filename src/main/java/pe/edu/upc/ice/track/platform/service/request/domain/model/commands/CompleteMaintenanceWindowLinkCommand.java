package pe.edu.upc.ice.track.platform.service.request.domain.model.commands;

import java.time.Instant;

/**
 * Command completing, on a service request, the link of a maintenance window that was closed.
 *
 * <p>Issued by the inbound adapter reacting to the maintenance window integration events, never by a
 * REST caller.</p>
 *
 * @param serviceRequestId the request the window was linked to; required
 * @param windowId         identity of the window; required
 * @param endedAt          when the window ended; required
 * @param closureReason    why it ended; required
 */
public record CompleteMaintenanceWindowLinkCommand(
    Long serviceRequestId, Long windowId, Instant endedAt, String closureReason) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public CompleteMaintenanceWindowLinkCommand {
    if (serviceRequestId == null || windowId == null || endedAt == null
        || closureReason == null || closureReason.isBlank()) {
      throw new IllegalArgumentException("serviceRequestId, windowId, endedAt and closureReason are required");
    }
  }
}
