package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PersonNamePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PhonePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.StreetAddressPersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.OwnerProfilePersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianProfilePersistenceEntity;

/**
 * Static assembler between the profile domain hierarchy and the profile persistence hierarchy.
 *
 * <p>Maps the attributes stored in the shared {@code profiles} table, and dispatches a polymorphic
 * entity to the assembler of its role.</p>
 */
public final class ProfilePersistenceAssembler {

  private ProfilePersistenceAssembler() {
  }

  /**
   * Converts a profile entity of any role into the matching concrete domain profile.
   *
   * @param entity the persistence entity, loaded through the root of the hierarchy
   * @return the concrete domain profile, or {@code null} when the entity is {@code null}
   * @throws IllegalStateException when the entity belongs to a role this assembler does not know
   */
  public static Profile toDomainFromPersistence(ProfilePersistenceEntity entity) {
    return switch (entity) {
      case null -> null;
      case OwnerProfilePersistenceEntity owner -> OwnerProfilePersistenceAssembler.toDomainFromPersistence(owner);
      case TechnicianProfilePersistenceEntity technician ->
          TechnicianProfilePersistenceAssembler.toDomainFromPersistence(technician);
      default -> throw new IllegalStateException("Unknown profile type: " + entity.getClass().getName());
    };
  }

  static PersonName toPersonName(ProfilePersistenceEntity entity) {
    var name = entity.getName();
    return new PersonName(name.getFirstName(), name.getLastName());
  }

  static Phone toPhone(ProfilePersistenceEntity entity) {
    var phone = entity.getPhone();
    return new Phone(phone.getCountryCode(), phone.getNumber());
  }

  static StreetAddress toStreetAddress(ProfilePersistenceEntity entity) {
    var address = entity.getStreetAddress();
    return new StreetAddress(address.getStreet(), address.getNumber(), address.getCity(), address.getPostalCode(),
        address.getCountry());
  }

  /**
   * Copies the attributes every profile shares onto a persistence entity.
   *
   * @param profile the domain profile
   * @param entity  the persistence entity of the matching role
   */
  static void copySharedAttributes(Profile profile, ProfilePersistenceEntity entity) {
    // Only set ID if the profile is being updated (has a non-null ID)
    // For new profiles, leave ID null to allow JPA to generate it
    if (profile.getUserProfileId() != null) {
      entity.setId(profile.getUserProfileId());
    }
    var name = profile.getFullName();
    var phone = profile.getPhone();
    var address = profile.getAddress();
    entity.setUserId(profile.getUserId().userId());
    entity.setName(new PersonNamePersistenceEmbeddable(name.firstName(), name.lastName()));
    entity.setEmailAddress(profile.getEmail());
    entity.setPhone(new PhonePersistenceEmbeddable(phone.countryCode(), phone.phoneNumber()));
    entity.setStreetAddress(new StreetAddressPersistenceEmbeddable(
        address.street(), address.number(), address.city(), address.postalCode(), address.country()));
  }
}
