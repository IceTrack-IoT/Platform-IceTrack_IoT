package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.TechnicianCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Technician profile.
 *
 * <p>The profile of an ice track maintenance technician. On top of the attributes every
 * {@link Profile} shares, a technician is only eligible for maintenance work with a
 * {@link Speciality} and a certification number.</p>
 *
 * <p>No JPA or persistence annotation is present here - those concerns live exclusively in
 * {@code TechnicianProfilePersistenceEntity}.</p>
 */
@Getter
public class TechnicianProfile extends Profile {

  /**
   * Format of a certification number: 3 to 50 letters, digits, {@code -}, {@code .} or
   * {@code /}, starting with a letter or digit.
   */
  public static final String CERTIFICATION_NUMBER_PATTERN = "^[A-Za-z0-9][A-Za-z0-9./-]{2,49}$";

  private static final Pattern CERTIFICATION_NUMBER_FORMAT = Pattern.compile(CERTIFICATION_NUMBER_PATTERN);

  private Speciality speciality;
  private String certificationNumber;

  /**
   * Creates a new, not yet persisted, technician profile.
   *
   * @param userId              identifier of the account the technician belongs to; required
   * @param fullName            the technician's name; required
   * @param email               the technician's email address; required
   * @param phone               the technician's phone number; required
   * @param address             the technician's street address; required
   * @param speciality          the technician's speciality; required
   * @param certificationNumber the number of the technician's certification; required
   */
  public TechnicianProfile(UserId userId, PersonName fullName, EmailAddress email, Phone phone, StreetAddress address,
                           Speciality speciality, String certificationNumber) {
    this(null, userId, fullName, email, phone, address, speciality, certificationNumber);
  }

  /**
   * Reconstitutes a technician profile.
   *
   * @param userProfileId       the persistence identity, or {@code null} for a profile not yet persisted
   * @param userId              identifier of the account the technician belongs to; required
   * @param fullName            the technician's name; required
   * @param email               the technician's email address; required
   * @param phone               the technician's phone number; required
   * @param address             the technician's street address; required
   * @param speciality          the technician's speciality; required
   * @param certificationNumber the number of the technician's certification; required
   */
  public TechnicianProfile(Long userProfileId, UserId userId, PersonName fullName, EmailAddress email, Phone phone,
                           StreetAddress address, Speciality speciality, String certificationNumber) {
    super(userProfileId, userId, fullName, email, phone, address);
    this.speciality = Objects.requireNonNull(speciality, "speciality must not be null");
    this.certificationNumber = requireCertificationNumber(certificationNumber);
  }

  /**
   * Records a new certification for this technician.
   *
   * <p>A speciality and its certification always change together: a technician is never left
   * with a speciality that no certification backs.</p>
   *
   * @param speciality          the certified speciality; required
   * @param certificationNumber the number of the new certification; required
   * @throws IllegalArgumentException when the certification number is blank or malformed
   */
  public void updateCertification(Speciality speciality, String certificationNumber) {
    var validatedCertificationNumber = requireCertificationNumber(certificationNumber);
    this.speciality = Objects.requireNonNull(speciality, "speciality must not be null");
    this.certificationNumber = validatedCertificationNumber;
  }

  /**
   * Checks whether this technician is certified for a speciality.
   *
   * <p>Speciality names are compared ignoring case, so {@code "Refrigeration"} and
   * {@code "refrigeration"} denote the same field.</p>
   *
   * @param speciality the speciality a maintenance task requires
   * @return {@code true} when this technician holds that speciality
   */
  public boolean isAvailableFor(Speciality speciality) {
    return speciality != null && this.speciality.name().equalsIgnoreCase(speciality.name());
  }

  // inherited javadoc
  @Override
  public void onCreated() {
    registerDomainEvent(TechnicianCreatedEvent.from(this));
  }

  private static String requireCertificationNumber(String certificationNumber) {
    if (certificationNumber == null || certificationNumber.isBlank()) {
      throw new IllegalArgumentException("Certification number must not be null or blank");
    }
    var trimmed = certificationNumber.trim();
    if (!CERTIFICATION_NUMBER_FORMAT.matcher(trimmed).matches()) {
      throw new IllegalArgumentException(
          "Certification number must be 3 to 50 letters, digits, '-', '.' or '/', starting with a letter or digit");
    }
    return trimmed;
  }
}
