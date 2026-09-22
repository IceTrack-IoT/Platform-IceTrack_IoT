package pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * ProfileCreationData Value Object.
 *
 * <p>Carries everything a {@link pe.edu.upc.ice.track.platform.profiles.domain.model.factories.UserProfileFactory}
 * needs to instantiate a {@link pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile}.
 * It is the translation target of the Anti-Corruption Layer: the
 * {@link pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade}
 * implementation maps the agnostic values it receives from other bounded contexts onto
 * {@link UserId}, {@link PersonName}, {@link EmailAddress}, {@link Phone} and
 * {@link ProfileRole} before building this record, so no foreign type ever reaches the profiles
 * domain.</p>
 *
 * <p>Contact details are optional. A profile born out of a federated registration only knows the
 * identity claims the provider released; the owner completes the phone number and the address
 * later through the profiles API.</p>
 *
 * @param userId        identifier of the account the profile belongs to; required
 * @param fullName      the profile owner's name; required
 * @param email         the profile owner's email address; required
 * @param role          the role the profile plays; required
 * @param phone         the profile owner's phone number; may be {@code null}
 * @param address       the profile owner's street address; may be {@code null}
 * @param auxiliaryData an optional opaque annotation supplied by the calling context, such as the
 *                      avatar URL released by an identity provider; may be {@code null}
 */
public record ProfileCreationData(
    UserId userId,
    PersonName fullName,
    EmailAddress email,
    ProfileRole role,
    Phone phone,
    StreetAddress address,
    String auxiliaryData) {

  /**
   * Validates the required components and normalises the optional annotation.
   */
  public ProfileCreationData {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(fullName, "fullName must not be null");
    Objects.requireNonNull(email, "email must not be null");
    Objects.requireNonNull(role, "role must not be null");
    auxiliaryData = auxiliaryData == null || auxiliaryData.isBlank() ? null : auxiliaryData.trim();
  }

  /**
   * Creates the minimal creation data released by a federated identity provider.
   *
   * @param userId   identifier of the account the profile belongs to
   * @param fullName the profile owner's name
   * @param email    the profile owner's email address
   * @param role     the role the profile plays
   */
  public ProfileCreationData(UserId userId, PersonName fullName, EmailAddress email, ProfileRole role) {
    this(userId, fullName, email, role, null, null, null);
  }
}
