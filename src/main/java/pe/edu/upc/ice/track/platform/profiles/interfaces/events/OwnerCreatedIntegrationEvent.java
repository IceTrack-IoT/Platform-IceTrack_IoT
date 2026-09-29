package pe.edu.upc.ice.track.platform.profiles.interfaces.events;

/**
 * Integration event published by the {@code profiles} bounded context when a new owner has been
 * created and persisted.
 *
 * <p>This is the <em>published language</em> of the {@code profiles} context: other bounded
 * contexts listen to this event rather than to the internal
 * {@link pe.edu.upc.ice.track.platform.profiles.domain.model.events.OwnerCreatedEvent}.</p>
 *
 * @param ownerId  the identity assigned to the newly created owner
 * @param userId   the identity of the platform account the owner belongs to
 * @param fullName the owner's full name
 * @param email    the owner's email address
 * @param ruc      the owner's taxpayer registration number
 */
public record OwnerCreatedIntegrationEvent(Long ownerId, Long userId, String fullName, String email, Long ruc) {
}
