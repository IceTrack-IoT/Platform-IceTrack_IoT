package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.OwnerProfilePersistenceEntity;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Static assembler between owner profile domain and persistence representations.
 */
public final class OwnerProfilePersistenceAssembler {

  private OwnerProfilePersistenceAssembler() {
  }

  public static OwnerProfile toDomainFromPersistence(OwnerProfilePersistenceEntity entity) {
    if (entity == null) return null;
    return new OwnerProfile(
        entity.getId(),
        new UserId(entity.getUserId()),
        ProfilePersistenceAssembler.toPersonName(entity),
        entity.getEmailAddress(),
        ProfilePersistenceAssembler.toPhone(entity),
        ProfilePersistenceAssembler.toStreetAddress(entity),
        new Ruc(entity.getRuc()));
  }

  public static OwnerProfilePersistenceEntity toPersistenceFromDomain(OwnerProfile owner) {
    if (owner == null) return null;
    var entity = new OwnerProfilePersistenceEntity();
    ProfilePersistenceAssembler.copySharedAttributes(owner, entity);
    entity.setRuc(owner.getRuc().value());
    return entity;
  }
}
