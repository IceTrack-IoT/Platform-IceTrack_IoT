package pe.edu.upc.ice.track.platform.profiles.interfaces.events;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;

/**
 * Integration event published by the {@code profiles} bounded context when a new
 * {@link Profile} has been successfully created and persisted.
 *
 * <p>This is the <em>published language</em> of the {@code profiles} context.
 * Other bounded contexts must listen to this event rather than to internal domain events such as
 * {@link pe.edu.upc.ice.track.platform.profiles.domain.model.events.ProfileCreatedEvent},
 * which is an internal concern of the {@code profiles} domain.</p>
 *
 * <p>Contact details are optional on a profile, so the corresponding components of this event
 * may be {@code null}.</p>
 *
 * @param profileId   The identity assigned to the newly created profile.
 * @param userId      The identity of the platform account the profile belongs to, may be {@code null}.
 * @param firstName   The profile owner's first name.
 * @param lastName    The profile owner's last name.
 * @param email       The profile owner's email address.
 * @param role        The role the profile plays.
 * @param countryCode The profile owner's country code, may be {@code null}.
 * @param phoneNumber The profile owner's phone number, may be {@code null}.
 * @param street      Street component of the profile's address, may be {@code null}.
 * @param number      Street number component of the profile's address, may be {@code null}.
 * @param city        City component of the profile's address, may be {@code null}.
 * @param postalCode  Postal code component of the profile's address, may be {@code null}.
 * @param country     Country component of the profile's address, may be {@code null}.
 */
public record ProfileCreatedIntegrationEvent(
    Long profileId,
    Long userId,
    String firstName,
    String lastName,
    String email,
    String role,
    String countryCode,
    String phoneNumber,
    String street,
    String number,
    String city,
    String postalCode,
    String country) {

  /**
   * Convenience factory that extracts all fields from a saved {@link Profile}.
   *
   * @param profile the saved profile (must already carry a non-null id)
   * @return a fully populated {@link ProfileCreatedIntegrationEvent}
   */
  public static ProfileCreatedIntegrationEvent from(Profile profile) {
    var name = profile.getFullName();
    var address = profile.getAddress();
    var phone = profile.getPhone();
    return new ProfileCreatedIntegrationEvent(
        profile.getId(),
        profile.getUserId() == null ? null : profile.getUserId().userId(),
        name.firstName(),
        name.lastName(),
        profile.getEmail().getAddress(),
        profile.getRoleName(),
        phone == null ? null : phone.countryCode(),
        phone == null ? null : phone.phoneNumber(),
        address == null ? null : address.street(),
        address == null ? null : address.number(),
        address == null ? null : address.city(),
        address == null ? null : address.postalCode(),
        address == null ? null : address.country());
  }
}
