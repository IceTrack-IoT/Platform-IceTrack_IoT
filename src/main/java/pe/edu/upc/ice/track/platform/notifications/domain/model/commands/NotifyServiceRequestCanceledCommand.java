package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/**
 * Command notifying the technician assigned to a service request that its owner canceled it.
 *
 * @param serviceRequestId    the request; required
 * @param technicianProfileId the technician assigned at the time; required
 * @param equipmentId         the unit the request was raised for; required
 * @param reason              why the owner canceled; required
 */
public record NotifyServiceRequestCanceledCommand(
    Long serviceRequestId,
    Long technicianProfileId,
    Long equipmentId,
    String reason) {

  /**
   * Validates the command.
   *
   * @throws IllegalArgumentException when a required value is missing
   */
  public NotifyServiceRequestCanceledCommand {
    if (serviceRequestId == null || technicianProfileId == null || equipmentId == null
        || reason == null || reason.isBlank()) {
      throw new IllegalArgumentException("Every field of a cancellation notification is required");
    }
  }
}
