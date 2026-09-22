package pe.edu.upc.ice.track.platform.iam.domain.repositories;

import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;

import java.util.List;
import java.util.Optional;

/**
 * User repository interface
 * <p>
 *     This interface represents the repository for the User aggregate.
 * </p>
 */
public interface UserRepository {
  /**
   * Find user by id
   *
   * @param id the id of the user
   * @return an optional containing the user if found, or empty if not found
   */
  Optional<User> findById(Long id);

  /**
   * Find user by username
   *
   * @param username the username of the user
   * @return an optional containing the user if found, or empty if not found
   */
  Optional<User> findByUsername(String username);

  /**
   * Find user by email address
   *
   * @param email the email address of the user
   * @return an optional containing the user if found, or empty if not found
   */
  Optional<User> findByEmail(String email);

  /**
   * Find user by the identifier assigned by an external identity provider.
   *
   * @param provider the identity provider that issued the identifier
   * @param externalId the identifier of the account at the external provider
   * @return an optional containing the user if found, or empty if not found
   */
  Optional<User> findByProviderAndExternalId(AuthProvider provider, String externalId);

  /**
   * Find all users
   *
   * @return a list of all users
   */
  List<User> findAll();

  /**
   * Save user
   *
   * @param user the user to save
   * @return the saved user
   */
  User save(User user);

  /**
   * Check if user exists by username
   *
   * @param username the username of the user
   * @return true if the user exists, false otherwise
   */
  boolean existsByUsername(String username);

  /**
   * Check if user exists by email address
   *
   * @param email the email address of the user
   * @return true if the user exists, false otherwise
   */
  boolean existsByEmail(String email);
}
