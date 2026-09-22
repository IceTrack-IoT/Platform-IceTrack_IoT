package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.RolePersistenceEntity;

/**
 * Static assembler between IAM role domain and persistence representations.
 */
public final class RolePersistenceAssembler {

  private RolePersistenceAssembler() {
  }

  public static Role toDomainFromPersistence(RolePersistenceEntity entity) {
    if (entity == null) return null;
    return new Role(entity.getId(), entity.getName());
  }

  public static RolePersistenceEntity toPersistenceFromDomain(Role role) {
    if (role == null) return null;
    var entity = new RolePersistenceEntity();
    // Only set ID if the role is being updated (has a non-null ID)
    // For new roles, leave ID null to allow JPA to generate it
    if (role.getId() != null) {
      entity.setId(role.getId());
    }
    entity.setName(role.getName());
    return entity;
  }
}