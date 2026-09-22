package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.RolePersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for IAM role persistence entities.
 */
@Repository
public interface RolePersistenceRepository extends JpaRepository<RolePersistenceEntity, Long> {

  /**
   * This method is responsible for finding the role by name.
   * @param name The role name.
   * @return The role object.
   */
  Optional<RolePersistenceEntity> findByName(Roles name);

  /**
   * This method is responsible for checking if the role exists by name.
   * @param name The role name.
   * @return True if the role exists, false otherwise.
   */
  boolean existsByName(Roles name);

}
