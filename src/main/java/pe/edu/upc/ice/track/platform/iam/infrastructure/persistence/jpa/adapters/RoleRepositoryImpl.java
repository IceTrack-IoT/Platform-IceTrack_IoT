package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.adapters;

import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.assemblers.RolePersistenceAssembler;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories.RolePersistenceRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository adapter that bridges the IAM role domain repository port with Spring Data JPA.
 */
@Repository
public class RoleRepositoryImpl implements RoleRepository {

  private final RolePersistenceRepository rolePersistenceRepository;

  public RoleRepositoryImpl(RolePersistenceRepository rolePersistenceRepository) {
    this.rolePersistenceRepository = rolePersistenceRepository;
  }

  @Override
  public Optional<Role> findByName(Roles name) {
    return rolePersistenceRepository.findByName(name).map(RolePersistenceAssembler::toDomainFromPersistence);
  }

  @Override
  public List<Role> findAll() {
    return rolePersistenceRepository.findAll().stream().map(RolePersistenceAssembler::toDomainFromPersistence).toList();
  }

  @Override
  public Role save(Role role) {
    var saved = rolePersistenceRepository.save(RolePersistenceAssembler.toPersistenceFromDomain(role));
    return RolePersistenceAssembler.toDomainFromPersistence(saved);
  }

  @Override
  public boolean existsByName(Roles name) {
    return rolePersistenceRepository.existsByName(name);
  }
}
