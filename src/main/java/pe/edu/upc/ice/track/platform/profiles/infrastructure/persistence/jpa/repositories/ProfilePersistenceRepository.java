package pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.infrastructure.persistence.jpa.entities.ProfilePersistenceEntity;

import java.util.Optional;

/**
 * Spring Data repository for profile persistence entities.
 */
@Repository
public interface ProfilePersistenceRepository extends JpaRepository<ProfilePersistenceEntity, Long> {

  /**
   * Find a profile by its email address.
   *
   * @param emailAddress The email address to search for.
   * @return An Optional containing the profile if found, or empty if not found.
   */
  @Query("select profile from ProfilePersistenceEntity profile where profile.emailAddress = :emailAddress")
  Optional<ProfilePersistenceEntity> findByEmailAddress(@Param("emailAddress") EmailAddress emailAddress);

  /**
   * Count the number of profiles with a given email address.
   *
   * @param emailAddress The email address to count profiles for.
   * @return The number of profiles with the given email address.
   */
  @Query("select count(profile) from ProfilePersistenceEntity profile where profile.emailAddress = :emailAddress")
  long countByEmailAddress(@Param("emailAddress") EmailAddress emailAddress);

  /**
   * Find the profile linked to a platform account.
   *
   * @param userId The identifier of the account the profile belongs to.
   * @return An Optional containing the profile if found, or empty if not found.
   */
  Optional<ProfilePersistenceEntity> findByUserId(Long userId);

  /**
   * Check whether a profile is already linked to a platform account.
   *
   * @param userId The identifier of the account to check.
   * @return True when a profile for the given account exists, false otherwise.
   */
  boolean existsByUserId(Long userId);
}
