package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
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
 * <p>Holds the attributes shared by every profile. The class is {@code abstract} and
 * {@code sealed}: the only profiles that can ever exist - and therefore be persisted - are an
 * {@link OwnerProfile} or a {@link TechnicianProfile}. There is no generic or provisional profile,
 * and the role is a property of the concrete type, so it can never drift away from the
 * role specific data.</p>
 *
 * <p>Extends {@link AbstractDomainAggregateRoot} to gain domain event registration support. No JPA
 * or persistence annotation is present here - those concerns live exclusively in
 * {@code ProfilePersistenceEntity}.</p>
 */
@Getter
public abstract sealed class Profile extends AbstractDomainAggregateRoot<Profile>
    permits OwnerProfile, TechnicianProfile {

  private final Long id;
  private final UserId userId;
  private final PersonName fullName;
  private final EmailAddress email;
  private Phone phone;
  private StreetAddress address;
  private final String auxiliaryData;

  /**
   * Builds the shared state of a profile.
   *
   * @param id   the persistence identity, or {@code null} for a profile not yet persisted
   * @param data the validated creation data; required
   */
  protected Profile(Long id, ProfileCreationData data) {
    Objects.requireNonNull(data, "profile creation data must not be null");
    this.id = id;
    this.userId = data.userId();
    this.fullName = data.fullName();
    this.email = data.email();
    this.phone = data.phone();
    this.address = data.address();
    this.auxiliaryData = data.auxiliaryData();
  }

  /**
   * Returns the role this profile plays, as determined by its concrete type.
   *
   * @return the role, never {@code null}
   */
  public abstract ProfileRole getRole();

  /**
   * Returns the name of the role this profile plays.
   *
   * @return the role name, never {@code null}
   */
  public String getRoleName() {
    return getRole().name();
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
   * Replaces the contact details of this profile.
   *
   * @param phone   the new phone number; required
   * @param address the new street address; required
   */
  public void updateContactDetails(Phone phone, StreetAddress address) {
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
    this.address = Objects.requireNonNull(address, "address must not be null");
  }
}
