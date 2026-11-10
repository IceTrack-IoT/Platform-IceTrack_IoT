package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import java.time.Instant;

/**
 * Command notifying a technician that a service request was assigned to it, and the technician it
 * replaced, if any, that it no longer is.
 *
 * @param serviceRequestId            the request; required
 * @param technicianProfileId         the technician now assigned; required
 * @param previousTechnicianProfileId the technician replaced, or {@code null}
 * @param equipmentId                 the unit to service; required
 * @param siteId                      the site the unit is installed at; required
 * @param serviceType                 the kind of work, as plain text; required
 * @param priority                    the urgency, as plain text; required
 * @param assignedAt                  when the assignment happened; required
 */
public record NotifyTechnicianAssignedCommand(
    Long serviceRequestId,
    Long technicianProfileId,
    Long previousTechnicianProfileId,
    Long equipmentId,
    Long siteId,
    String serviceType,
    String priority,
    Instant assignedAt) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public NotifyTechnicianAssignedCommand {
    if (serviceRequestId == null || technicianProfileId == null || equipmentId == null || siteId == null
        || serviceType == null || priority == null || assignedAt == null) {
      throw new IllegalArgumentException("Every field of a technician assignment notification is required");
    }
  }
}
