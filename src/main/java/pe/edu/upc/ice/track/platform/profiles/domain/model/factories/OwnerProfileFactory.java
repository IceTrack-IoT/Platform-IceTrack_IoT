package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;

/**
 * Builds the profile of an ice track owner.
 */
public class OwnerProfileFactory implements UserProfileFactory {

  // inherited javadoc
  @Override
  public ProfileRole supportedRole() {
    return ProfileRole.OWNER;
  }

  // inherited javadoc
  @Override
  public Profile createFrom(ProfileCreationData data) {
    return new Profile(data, ProfileRole.OWNER);
  }
}
