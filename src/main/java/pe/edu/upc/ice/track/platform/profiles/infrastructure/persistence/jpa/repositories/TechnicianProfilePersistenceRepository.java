package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.TechnicianProfilePersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for technician profile persistence entities.
 *
 * <p>Queries only return rows present in {@code technician_profiles}, joined with their
 * {@code profiles} row.</p>
 */
@Repository
public interface TechnicianProfilePersistenceRepository extends JpaRepository<TechnicianProfilePersistenceEntity, Long> {

  /**
   * Find the technician profile bound to a platform account.
   *
   * @param userId The identifier of the account.
   * @return An Optional containing the technician profile if found, or empty if not found.
   */
  Optional<TechnicianProfilePersistenceEntity> findByUserId(Long userId);
}
