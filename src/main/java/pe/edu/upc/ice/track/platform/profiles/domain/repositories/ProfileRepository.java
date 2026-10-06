package pe.edu.upc.ice.track.platform.profiles.domain.repositories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Profile repository port.
 *
 * <p>Works on the {@link Profile} hierarchy as a whole, whatever the role. It answers the
 * questions that span every role - an account has at most one profile, and an email address
 * belongs to at most one profile - with a single lookup on the shared {@code profiles} table.
 * Loading and saving a profile of a known role goes through {@link OwnerProfileRepository} or
 * {@link TechnicianProfileRepository}.</p>
 */
public interface ProfileRepository {

  /**
   * Find the profile bound to a platform account, whatever its role.
   *
   * @param userId the identifier of the account
   * @return the concrete profile, or empty when the account has none
   */
  Optional<Profile> findByUserId(UserId userId);

  /**
   * Check whether a profile of any role is bound to a platform account.
   *
   * @param userId the identifier of the account
   * @return true when the account has a profile
   */
  boolean existsByUserId(UserId userId);

  /**
   * Find the profile that uses an email address, whatever its role.
   *
   * @param emailAddress the email address
   * @return the concrete profile, or empty when no profile uses the email address
   */
  Optional<Profile> findByEmailAddress(EmailAddress emailAddress);

  /**
   * Check whether a profile of any role uses an email address.
   *
   * @param emailAddress the email address
   * @return true when a profile uses the email address
   */
  boolean existsByEmailAddress(EmailAddress emailAddress);
}
