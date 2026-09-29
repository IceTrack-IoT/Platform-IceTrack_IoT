package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for the root of the profile persistence hierarchy.
 *
 * <p>Queries run against the {@code profiles} table and load the concrete subclass entity, so
 * they cover every role at once.</p>
 */
@Repository
public interface ProfilePersistenceRepository extends JpaRepository<ProfilePersistenceEntity, Long> {

  /**
   * Find the profile bound to a platform account, whatever its role.
   *
   * @param userId The identifier of the account.
   * @return An Optional containing the concrete profile entity if found, or empty if not found.
   */
  Optional<ProfilePersistenceEntity> findByUserId(Long userId);

  /**
   * Check whether a profile of any role is bound to a platform account.
   *
   * @param userId The identifier of the account.
   * @return True when a profile for the given account exists, false otherwise.
   */
  boolean existsByUserId(Long userId);

  /**
   * Count the profiles of any role using a given email address.
   *
   * @param emailAddress The email address to count profiles for.
   * @return The number of profiles with the given email address.
   */
  @Query("select count(profile) from ProfilePersistenceEntity profile where profile.emailAddress = :emailAddress")
  long countByEmailAddress(@Param("emailAddress") EmailAddress emailAddress);
}
