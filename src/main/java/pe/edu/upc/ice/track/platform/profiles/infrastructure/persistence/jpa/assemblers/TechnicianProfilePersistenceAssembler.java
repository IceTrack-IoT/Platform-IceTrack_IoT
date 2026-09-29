package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianProfilePersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Static assembler between technician profile domain and persistence representations.
 */
public final class TechnicianProfilePersistenceAssembler {

  private TechnicianProfilePersistenceAssembler() {
  }

  public static TechnicianProfile toDomainFromPersistence(TechnicianProfilePersistenceEntity entity) {
    if (entity == null) return null;
    return new TechnicianProfile(
        entity.getId(),
        new UserId(entity.getUserId()),
        ProfilePersistenceAssembler.toPersonName(entity),
        entity.getEmailAddress(),
        ProfilePersistenceAssembler.toPhone(entity),
        ProfilePersistenceAssembler.toStreetAddress(entity),
        new Speciality(entity.getSpeciality()),
        entity.getCertificationNumber());
  }

  public static TechnicianProfilePersistenceEntity toPersistenceFromDomain(TechnicianProfile technician) {
    if (technician == null) return null;
    var entity = new TechnicianProfilePersistenceEntity();
    ProfilePersistenceAssembler.copySharedAttributes(technician, entity);
    entity.setSpeciality(technician.getSpeciality().name());
    entity.setCertificationNumber(technician.getCertificationNumber());
    return entity;
  }
}
