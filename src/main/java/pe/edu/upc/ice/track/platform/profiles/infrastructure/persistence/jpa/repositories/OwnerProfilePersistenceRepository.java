package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.OwnerProfilePersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for owner profile persistence entities.
 *
 * <p>Queries only return rows present in {@code owner_profiles}, joined with their
 * {@code profiles} row.</p>
 */
@Repository
public interface OwnerProfilePersistenceRepository extends JpaRepository<OwnerProfilePersistenceEntity, Long> {

  /**
   * Find the owner profile bound to a platform account.
   *
   * @param userId The identifier of the account.
   * @return An Optional containing the owner profile if found, or empty if not found.
   */
  Optional<OwnerProfilePersistenceEntity> findByUserId(Long userId);
}
