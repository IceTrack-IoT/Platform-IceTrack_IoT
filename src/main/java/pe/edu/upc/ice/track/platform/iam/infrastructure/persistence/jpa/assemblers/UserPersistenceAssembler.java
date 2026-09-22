package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers;

import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;

import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Static assembler between IAM user domain and persistence representations.
 */
public final class UserPersistenceAssembler {

  private UserPersistenceAssembler() {
  }

  public static User toDomainFromPersistence(UserPersistenceEntity entity) {
    if (entity == null) return null;
    var domain = new User();
    domain.setId(entity.getId());
    domain.setUsername(entity.getUsername());
    domain.setPassword(entity.getPassword());
    domain.setEmail(entity.getEmail());
    domain.setProvider(entity.getProvider() == null ? AuthProvider.LOCAL : entity.getProvider());
    domain.setExternalId(entity.getExternalId());
    domain.setRoles(entity.getRoles() == null
        ? new HashSet<>()
        : entity.getRoles().stream()
        .map(RolePersistenceAssembler::toDomainFromPersistence)
        .collect(Collectors.toSet()));
    return domain;
  }

  public static UserPersistenceEntity toPersistenceFromDomain(User user) {
    if (user == null) return null;
    var entity = new UserPersistenceEntity();
    // Only set ID if the user is being updated (has a non-null ID)
    // For new users, leave ID null to allow JPA to generate it
    if (user.getId() != null) {
      entity.setId(user.getId());
    }
    entity.setUsername(user.getUsername());
    // Federated accounts carry no local password; the column is not nullable, so the
    // aggregate's empty placeholder is persisted as-is and never matches a bcrypt hash.
    entity.setPassword(user.getPassword() == null ? "" : user.getPassword());
    entity.setEmail(user.getEmail());
    entity.setProvider(user.getProvider() == null ? AuthProvider.LOCAL : user.getProvider());
    entity.setExternalId(user.getExternalId());
    entity.setRoles(user.getRoles() == null
        ? new HashSet<>()
        : user.getRoles().stream()
        .map(RolePersistenceAssembler::toPersistenceFromDomain)
        .collect(Collectors.toSet()));
    return entity;
  }
}
