package pe.edu.upc.ice.track.platform.profiles.domain.repositories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.List;
import java.util.Optional;

/**
 * Owner profile repository port.
 */
public interface OwnerProfileRepository {

  /**
   * Find an owner profile by its identifier.
   *
   * @param userProfileId the profile identifier
   * @return the owner profile, or empty when not found or when the profile belongs to another role
   */
  Optional<OwnerProfile> findById(Long userProfileId);

  /**
   * Find the owner profile bound to a platform account.
   *
   * @param userId the identifier of the account
   * @return the owner profile, or empty when not found
   */
  Optional<OwnerProfile> findByUserId(UserId userId);

  /**
   * Find all owner profiles.
   *
   * @return every owner profile
   */
  List<OwnerProfile> findAll();

  /**
   * Save an owner profile.
   *
   * @param owner the owner profile to save
   * @return the saved owner profile
   */
  OwnerProfile save(OwnerProfile owner);
}
