package pe.edu.upc.ice.track.platform.profiles.domain.model.events;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;

/**
 * Domain event published when a new {@link TechnicianProfile} is successfully created and
 * persisted.
 *
 * <p>Other bounded contexts must not subscribe to this event directly; it is translated into the
 * published language of the {@code profiles} context by
 * {@code profiles.application.internal.eventhandlers.TechnicianCreatedEventHandler}.</p>
 *
 * @param technicianId        the identity assigned to the newly created technician profile
 * @param userId              the identity of the platform account the technician belongs to
 * @param fullName            the technician's full name
 * @param email               the technician's email address
 * @param speciality          the technician's speciality
 * @param certificationNumber the number of the technician's certification
 */
public record TechnicianCreatedEvent(
    Long technicianId,
    Long userId,
    String fullName,
    String email,
    String speciality,
    String certificationNumber) {

  /**
   * Extracts the event fields from a saved {@link TechnicianProfile}.
   *
   * @param technician the saved technician profile (must already carry a non-null id)
   * @return the populated event
   */
  public static TechnicianCreatedEvent from(TechnicianProfile technician) {
    return new TechnicianCreatedEvent(
        technician.getUserProfileId(),
        technician.getUserId().userId(),
        technician.getFullName().getFullName(),
        technician.getEmail().getAddress(),
        technician.getSpeciality().name(),
        technician.getCertificationNumber());
  }
}
