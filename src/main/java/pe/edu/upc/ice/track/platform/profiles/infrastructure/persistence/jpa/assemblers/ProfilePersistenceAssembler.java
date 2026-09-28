package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianQualification;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PersonNamePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.PhonePersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.embeddables.StreetAddressPersistenceEmbeddable;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Static assembler between profile domain and persistence representations.
 *
 * <p>The {@code role} column selects the concrete profile to reconstitute. A row whose role is
 * missing, or whose role specific columns are empty, is rejected rather than turned into a
 * partial profile.</p>
 */
public final class ProfilePersistenceAssembler {

  private ProfilePersistenceAssembler() {
  }

  public static Profile toDomainFromPersistence(ProfilePersistenceEntity entity) {
    if (entity == null) return null;
    if (entity.getRole() == null) {
      throw new IllegalStateException("Profile %s has no role".formatted(entity.getId()));
    }
    var creationData = new ProfileCreationData(
        new UserId(entity.getUserId()),
        toDomainFromPersistence(entity.getName()),
        entity.getEmailAddress(),
        toDomainFromPersistence(entity.getPhone()),
        toDomainFromPersistence(entity.getStreetAddress()),
        entity.getAuxiliaryData());
    return switch (entity.getRole()) {
      case OWNER -> new OwnerProfile(entity.getId(), creationData, new Ruc(entity.getRuc()));
      case TECHNICIAN -> new TechnicianProfile(
          entity.getId(),
          creationData,
          new TechnicianQualification(entity.getSpeciality(), entity.getCertificationNumber()));
    };
  }

  public static ProfilePersistenceEntity toPersistenceFromDomain(Profile profile) {
    if (profile == null) return null;
    var entity = new ProfilePersistenceEntity();
    // Only set ID if the profile is being updated (has a non-null ID)
    // For new profiles, leave ID null to allow JPA to generate it
    if (profile.getId() != null) {
      entity.setId(profile.getId());
    }
    entity.setUserId(profile.getUserId().userId());
    entity.setName(toPersistenceFromDomain(profile.getFullName()));
    entity.setEmailAddress(profile.getEmail());
    entity.setRole(profile.getRole());
    entity.setPhone(toPersistenceFromDomain(profile.getPhone()));
    entity.setStreetAddress(toPersistenceFromDomain(profile.getAddress()));
    entity.setAuxiliaryData(profile.getAuxiliaryData());
    switch (profile) {
      case OwnerProfile ownerProfile -> entity.setRuc(ownerProfile.getRuc().value());
      case TechnicianProfile technicianProfile -> {
        entity.setSpeciality(technicianProfile.getQualification().speciality());
        entity.setCertificationNumber(technicianProfile.getQualification().certificationNumber());
      }
    }
    return entity;
  }

  private static PersonName toDomainFromPersistence(PersonNamePersistenceEmbeddable value) {
    return value == null ? null : new PersonName(value.getFirstName(), value.getLastName());
  }

  private static StreetAddress toDomainFromPersistence(StreetAddressPersistenceEmbeddable value) {
    if (value == null || value.getStreet() == null) return null;
    return new StreetAddress(value.getStreet(), value.getNumber(), value.getCity(), value.getPostalCode(), value.getCountry());
  }

  private static Phone toDomainFromPersistence(PhonePersistenceEmbeddable value) {
    if (value == null || value.getNumber() == null) return null;
    return new Phone(value.getCountryCode(), value.getNumber());
  }

  private static PersonNamePersistenceEmbeddable toPersistenceFromDomain(PersonName value) {
    return value == null ? null : new PersonNamePersistenceEmbeddable(value.firstName(), value.lastName());
  }

  private static StreetAddressPersistenceEmbeddable toPersistenceFromDomain(StreetAddress value) {
    return value == null ? null : new StreetAddressPersistenceEmbeddable(value.street(), value.number(), value.city(), value.postalCode(), value.country());
  }

  private static PhonePersistenceEmbeddable toPersistenceFromDomain(Phone value) {
    return value == null ? null : new PhonePersistenceEmbeddable(value.countryCode(), value.phoneNumber());
  }

}
