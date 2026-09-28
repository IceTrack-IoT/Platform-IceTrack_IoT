package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;

/**
 * Builds the profile of an ice track owner.
 */
public class OwnerProfileFactory implements UserProfileFactory<OwnerProfile, Ruc> {

  // inherited javadoc
  @Override
  public ProfileRole supportedRole() {
    return ProfileRole.OWNER;
  }

  // inherited javadoc
  @Override
  public OwnerProfile create(ProfileCreationData data, Ruc ruc) {
    return new OwnerProfile(data, ruc);
  }
}
