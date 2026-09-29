package pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates;

import lombok.Getter;
import pe.edu.upc.ice.track.platform.profiles.domain.model.events.OwnerCreatedEvent;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Owner profile.
 *
 * <p>The profile of an ice track owner. On top of the attributes every {@link Profile} shares, an
 * owner is always identified by its taxpayer registration number ({@link Ruc}).</p>
 *
 * <p>No JPA or persistence annotation is present here - those concerns live exclusively in
 * {@code OwnerProfilePersistenceEntity}.</p>
 */
@Getter
public class OwnerProfile extends Profile {

  private Ruc ruc;

  /**
   * Creates a new, not yet persisted, owner profile.
   *
   * @param userId   identifier of the account the owner belongs to; required
   * @param fullName the owner's name; required
   * @param email    the owner's email address; required
   * @param phone    the owner's phone number; required
   * @param address  the owner's street address; required
   * @param ruc      the owner's taxpayer registration number; required
   */
  public OwnerProfile(UserId userId, PersonName fullName, EmailAddress email, Phone phone, StreetAddress address,
                      Ruc ruc) {
    this(null, userId, fullName, email, phone, address, ruc);
  }

  /**
   * Reconstitutes an owner profile.
   *
   * @param userProfileId the persistence identity, or {@code null} for a profile not yet persisted
   * @param userId        identifier of the account the owner belongs to; required
   * @param fullName      the owner's name; required
   * @param email         the owner's email address; required
   * @param phone         the owner's phone number; required
   * @param address       the owner's street address; required
   * @param ruc           the owner's taxpayer registration number; required
   */
  public OwnerProfile(Long userProfileId, UserId userId, PersonName fullName, EmailAddress email, Phone phone,
                      StreetAddress address, Ruc ruc) {
    super(userProfileId, userId, fullName, email, phone, address);
    this.ruc = Objects.requireNonNull(ruc, "ruc must not be null");
  }

  /**
   * Replaces the taxpayer registration number of this owner.
   *
   * <p>An owner can never be left without a RUC: the new number replaces the old one and is
   * itself validated by {@link Ruc}.</p>
   *
   * @param ruc the new taxpayer registration number; required
   */
  public void updateTaxRegistration(Ruc ruc) {
    this.ruc = Objects.requireNonNull(ruc, "ruc must not be null");
  }

  // inherited javadoc
  @Override
  public void onCreated() {
    registerDomainEvent(OwnerCreatedEvent.from(this));
  }
}
