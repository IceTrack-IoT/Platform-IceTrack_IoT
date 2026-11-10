package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/**
 * Command notifying an owner that the technician assigned to one of its service requests rejected
 * it.
 *
 * @param serviceRequestId    the request; required
 * @param ownerProfileId      the owner of the request; required
 * @param technicianProfileId the technician who rejected; required
 * @param equipmentId         the unit to service; required
 * @param reason              why the technician rejected; required
 */
public record NotifyServiceRequestRejectedCommand(
    Long serviceRequestId,
    Long ownerProfileId,
    Long technicianProfileId,
    Long equipmentId,
    String reason) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public NotifyServiceRequestRejectedCommand {
    if (serviceRequestId == null || ownerProfileId == null || technicianProfileId == null || equipmentId == null
        || reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Every field of a rejection notification is required");
    }
  }
}
