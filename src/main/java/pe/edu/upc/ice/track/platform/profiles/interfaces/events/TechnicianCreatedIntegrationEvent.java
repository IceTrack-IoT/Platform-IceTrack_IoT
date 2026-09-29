package pe.edu.upc.ice.track.platform.profiles.interfaces.events;

/**
 * Integration event published by the {@code profiles} bounded context when a new technician has
 * been created and persisted.
 *
 * <p>This is the <em>published language</em> of the {@code profiles} context: other bounded
 * contexts listen to this event rather than to the internal
 * {@link pe.edu.upc.ice.track.platform.profiles.domain.model.events.TechnicianCreatedEvent}.</p>
 *
 * @param technicianId        the identity assigned to the newly created technician
 * @param userId              the identity of the platform account the technician belongs to
 * @param fullName            the technician's full name
 * @param email               the technician's email address
 * @param speciality          the technician's speciality
 * @param certificationNumber the number of the technician's certification
 */
public record TechnicianCreatedIntegrationEvent(
    Long technicianId,
    Long userId,
    String fullName,
    String email,
    String speciality,
    String certificationNumber) {
}
