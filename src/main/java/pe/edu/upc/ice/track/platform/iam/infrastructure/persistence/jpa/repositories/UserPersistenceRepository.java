package pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.infrastructure.persistence.jpa.entities.UserPersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for IAM user persistence entities.
 */
@Repository
public interface UserPersistenceRepository extends JpaRepository<UserPersistenceEntity, Long>
{
  /**
   * This method is responsible for finding the user by username.
   * @param username The username.
   * @return The user object.
   */
  Optional<UserPersistenceEntity> findByUsername(String username);

  /**
   * This method is responsible for finding the user by its external provider identity.
   * @param provider The identity provider.
   * @param externalId The identifier of the account at the external provider.
   * @return The user object.
   */
  Optional<UserPersistenceEntity> findByProviderAndExternalId(AuthProvider provider, String externalId);

  /**
   * This method is responsible for checking if the user exists by username.
   * @param username The username.
   * @return True if the user exists, false otherwise.
   */
  boolean existsByUsername(String username);

}
