package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;

/**
 * Creates the {@link Profile} of a platform account.
 *
 * <p>There is one implementation per role. Each one narrows the return type to its concrete
 * profile, so a caller holding the specific factory gets the specific profile back without a
 * cast.</p>
 */
public interface UserProfileFactory {

  /**
   * Creates a new, not yet persisted, profile.
   *
   * @param userId identifier of the account the profile belongs to; required
   * @param data   the attributes of the profile; required
   * @return the new profile
   * @throws NullPointerException     when a required value is missing
   * @throws IllegalArgumentException when a value violates a profile invariant
   */
  Profile createProfile(Long userId, ProfileCreationData data);
}
