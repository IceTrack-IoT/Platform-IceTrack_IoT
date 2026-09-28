package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * ProfileCreationData Value Object.
 *
 * <p>Carries the attributes shared by every concrete profile, and is what a
 * {@link pe.edu.upc.ice.track.platform.profiles.domain.model.factories.UserProfileFactory} receives
 * alongside the role specific data. It is the translation target of the Anti-Corruption Layer: the
 * {@link pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade}
 * implementation maps the agnostic values it receives from other bounded contexts onto
 * {@link UserId}, {@link PersonName}, {@link EmailAddress}, {@link Phone} and
 * {@link StreetAddress} before building this record, so no foreign type ever reaches the profiles
 * domain.</p>
 *
 * <p>Every profile is born out of a completed onboarding form, so the contact details are
 * mandatory.</p>
 *
 * @param userId        identifier of the account the profile belongs to; required
 * @param fullName      the profile owner's name; required
 * @param email         the profile owner's email address; required
 * @param phone         the profile owner's phone number; required
 * @param address       the profile owner's street address; required
 * @param auxiliaryData an optional opaque annotation supplied by the calling context; may be
 *                      {@code null}
 */
public record ProfileCreationData(
    UserId userId,
    PersonName fullName,
    EmailAddress email,
    Phone phone,
    StreetAddress address,
    String auxiliaryData) {

  /**
   * Validates the required components and normalises the optional annotation.
   */
  public ProfileCreationData {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(userId.userId(), "userId must carry an identifier");
    Objects.requireNonNull(fullName, "fullName must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(phone, "phone must not be null");
    Objects.requireNonNull(address, "address must not be null");
    auxiliaryData = auxiliaryData == null || auxiliaryData.isBlank() ? null : auxiliaryData.trim();
  }

  /**
   * Creates the creation data without an auxiliary annotation.
   *
   * @param userId   identifier of the account the profile belongs to
   * @param fullName the profile owner's name
   * @param email    the profile owner's email address
   * @param phone    the profile owner's phone number
   * @param address  the profile owner's street address
   */
  public ProfileCreationData(UserId userId, PersonName fullName, EmailAddress email, Phone phone, StreetAddress address) {
    this(userId, fullName, email, phone, address, null);
  }
}
