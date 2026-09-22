package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
import lombok.Setter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.ProfileCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Profile aggregate root.
 *
 * <p>Extends {@link AbstractDomainAggregateRoot} to gain domain event registration
 * support. No JPA or persistence annotation is present here - those concerns live
 * exclusively in {@code ProfilePersistenceEntity}.</p>
 *
 * <p>Name, email and role are mandatory; the phone number and the street address are optional,
 * because a profile created as the consequence of a federated registration only knows the
 * identity claims the provider released. The owner completes them afterward.</p>
 */
@Getter
@Setter
public class Profile extends AbstractDomainAggregateRoot<Profile> {

  private Long id;
  private UserId userId;
  private PersonName fullName;
  private EmailAddress email;
  private ProfileRole role;
  private Phone phone;
  private StreetAddress address;
  private String auxiliaryData;

  public Profile(Long id, UserId userId, PersonName fullName, EmailAddress email, ProfileRole role, Phone phone, StreetAddress address) {
    this(id, userId, fullName, email, role, phone, address, null);
  }

  public Profile(Long id, UserId userId, PersonName fullName, EmailAddress email, ProfileRole role, Phone phone, StreetAddress address, String auxiliaryData) {
    this.id = id;
    this.userId = userId;
    this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
    this.email = Objects.requireNonNull(email, "email must not be null");
    this.role = role == null ? ProfileRole.USER : role;
    this.phone = phone;
    this.address = address;
    this.auxiliaryData = auxiliaryData;
  }

  public Profile(PersonName fullName, EmailAddress email, Phone phone, StreetAddress address) {
    this(null, null, fullName, email, ProfileRole.USER, phone, address);
  }

  public Profile(Long userId, String firstName, String lastName, String email, String countryCode, String phoneNumber, String street, String number, String city, String postalCode, String country) {
    this(null,
        userId == null ? null : new UserId(userId),
        new PersonName(firstName, lastName),
        new EmailAddress(email),
        ProfileRole.USER,
        toPhoneOrNull(countryCode, phoneNumber),
        toStreetAddressOrNull(street, number, city, postalCode, country));
  }

  public Profile(CreateProfileCommand command) {
    this(
        command.userId(),
        command.firstName(),
        command.lastName(),
        command.email(),
        command.countryCode(),
        command.phoneNumber(),
        command.street(),
        command.number(),
        command.city(),
        command.postalCode(),
        command.country());
  }

  /**
   * Creates a profile out of the data supplied by a {@code UserProfileFactory}.
   *
   * @param data the validated creation data
   * @param role the role stamped by the factory
   */
  public Profile(ProfileCreationData data, ProfileRole role) {
    this(null,
        Objects.requireNonNull(data, "profile creation data must not be null").userId(),
        data.fullName(),
        data.email(),
        role,
        data.phone(),
        data.address(),
        data.auxiliaryData());
  }

  /**
   * Signals that this profile has just been created and persisted.
   *
   * <p>Called by the repository adapter after the JPA identity has been assigned.
   * Registers a {@link ProfileCreatedEvent} so the infrastructure can publish it
   * to interested subscribers in other bounded contexts.</p>
   */
  public void onCreated() {
    registerDomainEvent(ProfileCreatedEvent.from(this));
  }

  /**
   * Binds this profile to a platform account.
   *
   * <p>Only a profile that is not yet linked may be bound, so an established ownership can never
   * be silently transferred to another account.</p>
   *
   * @param userId identifier of the account to bind this profile to; required
   * @throws IllegalStateException when the profile already belongs to a different account
   */
  public void linkToUser(UserId userId) {
    Objects.requireNonNull(userId, "userId must not be null");
    if (this.userId != null && !this.userId.equals(userId)) {
      throw new IllegalStateException("Profile %s already belongs to another user".formatted(id));
    }
    this.userId = userId;
  }

  /**
   * Replaces the contact details of this profile.
   *
   * @param phone   the new phone number, may be {@code null} to clear it
   * @param address the new street address, may be {@code null} to clear it
   */
  public void updateContactDetails(Phone phone, StreetAddress address) {
    this.phone = phone;
    this.address = address;
  }

  /**
   * Returns the name of the role this profile plays.
   *
   * @return the role name, never {@code null}
   */
  public String getRoleName() {
    return role == null ? ProfileRole.USER.name() : role.name();
  }

  /**
   * Builds a phone number, tolerating the absence of contact details.
   *
   * @param countryCode the country code
   * @param phoneNumber the phone number
   * @return the phone number, or {@code null} when either component is missing
   */
  private static Phone toPhoneOrNull(String countryCode, String phoneNumber) {
    if (countryCode == null || countryCode.isBlank() || phoneNumber == null || phoneNumber.isBlank()) {
      return null;
    }
    return new Phone(countryCode, phoneNumber);
  }

  /**
   * Builds a street address, tolerating the absence of contact details.
   *
   * @param street the street name
   * @param number the street number
   * @param city the city
   * @param postalCode the postal code
   * @param country the country
   * @return the street address, or {@code null} when any required component is missing
   */
  private static StreetAddress toStreetAddressOrNull(String street, String number, String city, String postalCode, String country) {
    if (street == null || street.isBlank()
        || city == null || city.isBlank()
        || postalCode == null || postalCode.isBlank()
        || country == null || country.isBlank()) {
      return null;
    }
    return new StreetAddress(street, number, city, postalCode, country);
  }

}
