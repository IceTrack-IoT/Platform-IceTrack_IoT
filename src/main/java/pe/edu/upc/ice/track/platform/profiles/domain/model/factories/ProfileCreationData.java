package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;

import java.util.Objects;

/**
 * Everything a {@link UserProfileFactory} needs to create a profile, already translated into
 * profiles value objects.
 *
 * <p>The shared attributes are always required. The role attributes are only filled for the role
 * being created - {@code ruc} for an owner, {@code speciality} and {@code certificationNumber} for a
 * technician - and each factory checks the ones it needs. Use {@link #forOwner} and
 * {@link #forTechnician} rather than the canonical constructor. This type never leaves the domain,
 * so the unused role attributes are never serialized anywhere.</p>
 *
 * @param fullName            the profile holder's name; required
 * @param email               the profile holder's email address; required
 * @param phone               the profile holder's phone number; required
 * @param address             the profile holder's street address; required
 * @param ruc                 the owner's taxpayer registration number; owners only
 * @param speciality          the technician's speciality; technicians only
 * @param certificationNumber the number of the technician's certification; technicians only
 */
public record ProfileCreationData(
    PersonName fullName,
    EmailAddress email,
    Phone phone,
    StreetAddress address,
    Ruc ruc,
    Speciality speciality,
    String certificationNumber) {

  /**
   * Validates that every shared attribute was supplied.
   */
  public ProfileCreationData {
    Objects.requireNonNull(fullName, "fullName must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(phone, "phone must not be null");
    Objects.requireNonNull(address, "address must not be null");
  }

  /**
   * Builds the creation data of an owner profile.
   *
   * @param fullName the owner's name; required
   * @param email    the owner's email address; required
   * @param phone    the owner's phone number; required
   * @param address  the owner's street address; required
   * @param ruc      the owner's taxpayer registration number; required
   * @return the creation data
   */
  public static ProfileCreationData forOwner(PersonName fullName, EmailAddress email, Phone phone,
                                             StreetAddress address, Ruc ruc) {
    return new ProfileCreationData(fullName, email, phone, address,
        Objects.requireNonNull(ruc, "ruc must not be null"), null, null);
  }

  /**
   * Builds the creation data of a technician profile.
   *
   * @param fullName            the technician's name; required
   * @param email               the technician's email address; required
   * @param phone               the technician's phone number; required
   * @param address             the technician's street address; required
   * @param speciality          the technician's speciality; required
   * @param certificationNumber the number of the technician's certification; required
   * @return the creation data
   */
  public static ProfileCreationData forTechnician(PersonName fullName, EmailAddress email, Phone phone,
                                                  StreetAddress address, Speciality speciality,
                                                  String certificationNumber) {
    return new ProfileCreationData(fullName, email, phone, address, null,
        Objects.requireNonNull(speciality, "speciality must not be null"), certificationNumber);
  }
}
