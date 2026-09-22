package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

/**
 * CreateProfileCommand record that represents the command to create a new profile from the
 * profiles REST API, with full contact details supplied by the caller.
 *
 * @param userId      identifier of the account the profile belongs to; may be {@code null} when
 *                    the profile is not linked to a platform account
 * @param firstName   the profile owner's first name
 * @param lastName    the profile owner's last name
 * @param email       the profile owner's email address
 * @param countryCode country code of the profile owner's phone number
 * @param phoneNumber phone number of the profile owner
 * @param street      street component of the profile's address
 * @param number      street number component of the profile's address
 * @param city        city component of the profile's address
 * @param postalCode  postal code component of the profile's address
 * @param country     country component of the profile's address
 */
public record CreateProfileCommand(Long userId,
                                   String firstName,
                                   String lastName,
                                   String email,
                                   String countryCode,
                                   String phoneNumber,
                                   String street,
                                   String number,
                                   String city,
                                   String postalCode,
                                   String country) {
}
