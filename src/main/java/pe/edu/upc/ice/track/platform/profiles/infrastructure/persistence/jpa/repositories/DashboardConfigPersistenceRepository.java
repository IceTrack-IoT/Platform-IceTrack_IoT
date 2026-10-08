package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.DashboardConfigPersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for dashboard configuration persistence entities.
 *
 * <p>The finders fetch the {@code cards} collection in the same query, since a configuration is
 * always turned into a whole aggregate. There is deliberately no repository for the cards.</p>
 */
@Repository
public interface DashboardConfigPersistenceRepository extends JpaRepository<DashboardConfigPersistenceEntity, Long> {

  /**
   * Find a dashboard configuration together with its cards.
   *
   * @param id The identifier of the configuration.
   * @return An Optional containing the configuration if found, or empty if not found.
   */
  @EntityGraph(attributePaths = "cards")
  Optional<DashboardConfigPersistenceEntity> findWithCardsById(Long id);

  /**
   * Find the dashboard configuration of a platform account together with its cards.
   *
   * @param userId The identifier of the account.
   * @return An Optional containing the configuration if found, or empty if not found.
   */
  @EntityGraph(attributePaths = "cards")
  Optional<DashboardConfigPersistenceEntity> findWithCardsByUserId(Long userId);

  /**
   * Check whether a platform account already has a dashboard configuration.
   *
   * @param userId The identifier of the account.
   * @return true when the account has a configuration.
   */
  boolean existsByUserId(Long userId);
}
