package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Profile aggregate root.
 *
 * <p>Abstract base of every user profile of the platform. It holds the data shared by all the
 * roles - the account binding, the name and the contact details - while each concrete role
 * ({@link OwnerProfile}, {@link TechnicianProfile}) adds only the attributes that make sense for
 * it, so no profile ever carries a value that belongs to another role.</p>
 *
 * <p>No JPA or persistence annotation is present here - the joined-table inheritance mapping
 * lives exclusively in {@code ProfilePersistenceEntity} and its subclasses.</p>
 */
@Getter
public abstract class Profile extends AbstractDomainAggregateRoot<Profile> {

  private final Long userProfileId;
  private final UserId userId;
  private PersonName fullName;
  private final EmailAddress email;
  private Phone phone;
  private StreetAddress address;

  /**
   * Initializes the attributes shared by every profile.
   *
   * @param userProfileId the persistence identity, or {@code null} for a profile not yet persisted
   * @param userId        identifier of the account the profile belongs to; required
   * @param fullName      the profile holder's name; required
   * @param email         the profile holder's email address; required
   * @param phone         the profile holder's phone number; required
   * @param address       the profile holder's street address; required
   */
  protected Profile(Long userProfileId, UserId userId, PersonName fullName, EmailAddress email, Phone phone,
                    StreetAddress address) {
    Objects.requireNonNull(userId, "userId must not be null");
    Objects.requireNonNull(userId.userId(), "userId must carry an identifier");
    this.userProfileId = userProfileId;
    this.userId = userId;
    this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
    this.email = Objects.requireNonNull(email, "email must not be null");
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
    this.address = Objects.requireNonNull(address, "address must not be null");
  }

  /**
   * Replaces the name and contact details of this profile.
   *
   * <p>The email address is not part of the update: it identifies the account holder across
   * bounded contexts - IAM resolves accounts by it - and is only ever set when the profile is
   * created.</p>
   *
   * @param fullName the new name; required
   * @param phone    the new phone number; required
   * @param address  the new street address; required
   */
  public void updateInfo(PersonName fullName, Phone phone, StreetAddress address) {
    this.fullName = Objects.requireNonNull(fullName, "fullName must not be null");
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
    this.address = Objects.requireNonNull(address, "address must not be null");
  }

  /**
   * Signals that this profile has just been created and persisted.
   *
   * <p>Called by the repository adapter after the persistence identity has been assigned. Each
   * role registers its own creation event.</p>
   */
  public abstract void onCreated();
}
