package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;

/**
 * Builds the profile of a platform user with no specialised responsibility.
 *
 * <p>This is the fallback of {@link UserProfileFactory#forRole}, applied whenever the role
 * carried by an incoming registration is the default one or could not be recognised.</p>
 */
public class StandardProfileFactory implements UserProfileFactory {

  // inherited javadoc
  @Override
  public ProfileRole supportedRole() {
    return ProfileRole.USER;
  }

  // inherited javadoc
  @Override
  public Profile createFrom(ProfileCreationData data) {
    return new Profile(data, ProfileRole.USER);
  }
}
