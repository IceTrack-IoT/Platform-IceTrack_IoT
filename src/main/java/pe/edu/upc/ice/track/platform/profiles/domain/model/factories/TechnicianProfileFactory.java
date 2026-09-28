package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianQualification;

/**
 * Builds the profile of an ice track maintenance technician.
 */
public class TechnicianProfileFactory implements UserProfileFactory<TechnicianProfile, TechnicianQualification> {

  // inherited javadoc
  @Override
  public ProfileRole supportedRole() {
    return ProfileRole.TECHNICIAN;
  }

  // inherited javadoc
  @Override
  public TechnicianProfile create(ProfileCreationData data, TechnicianQualification qualification) {
    return new TechnicianProfile(data, qualification);
  }
}
