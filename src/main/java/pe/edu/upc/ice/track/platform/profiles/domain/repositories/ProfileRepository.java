package pe.edu.upc.ice.track.platform.profiles.domain.repositories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Profile repository port.
 */
public interface ProfileRepository {
  /**
   * Find a profile by its ID.
   *
   * @param id the ID of the profile to find
   * @return an {@link Optional} containing the profile if found, or empty if not found
   */
  Optional<Profile> findById(Long id);

  /**
   * Find a profile by its email address.
   *
   * @param emailAddress the email address of the profile to find
   * @return an {@link Optional} containing the profile if found, or empty if not found
   */
  Optional<Profile> findByEmailAddress(EmailAddress emailAddress);

  /**
   * Find the profile linked to a platform account.
   *
   * @param userId the identifier of the account the profile belongs to
   * @return an {@link Optional} containing the profile if found, or empty if not found
   */
  Optional<Profile> findByUserId(UserId userId);

  /**
   * Find all profiles.
   *
   * @return a list of all profiles
   */
  List<Profile> findAll();

  /**
   * Save a profile.
   *
   * @param profile the profile to save
   * @return the saved profile
   */
  Profile save(Profile profile);

  /**
   * Check if a profile exists by its email address.
   *
   * @param emailAddress the email address to check for existence
   * @return true if a profile with the given email address exists, false otherwise
   */
  boolean existsByEmailAddress(EmailAddress emailAddress);

  /**
   * Check if a profile is already linked to a platform account.
   *
   * @param userId the identifier of the account to check for existence
   * @return true if a profile for the given account exists, false otherwise
   */
  boolean existsByUserId(UserId userId);
}
