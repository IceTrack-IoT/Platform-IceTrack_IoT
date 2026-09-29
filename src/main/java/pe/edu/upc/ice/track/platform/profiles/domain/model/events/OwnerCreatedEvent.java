package pe.edu.upc.ice.track.platform.profiles.domain.model.events;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;

/**
 * Domain event published when a new {@link OwnerProfile} is successfully created and persisted.
 *
 * <p>Other bounded contexts must not subscribe to this event directly; it is translated into the
 * published language of the {@code profiles} context by
 * {@code profiles.application.internal.eventhandlers.OwnerCreatedEventHandler}.</p>
 *
 * @param ownerId  the identity assigned to the newly created owner profile
 * @param userId   the identity of the platform account the owner belongs to
 * @param fullName the owner's full name
 * @param email    the owner's email address
 * @param ruc      the owner's taxpayer registration number
 */
public record OwnerCreatedEvent(Long ownerId, Long userId, String fullName, String email, Long ruc) {

  /**
   * Extracts the event fields from a saved {@link OwnerProfile}.
   *
   * @param owner the saved owner profile (must already carry a non-null id)
   * @return the populated event
   */
  public static OwnerCreatedEvent from(OwnerProfile owner) {
    return new OwnerCreatedEvent(
        owner.getUserProfileId(),
        owner.getUserId().userId(),
        owner.getFullName().getFullName(),
        owner.getEmail().getAddress(),
        owner.getRuc().value());
  }
}
