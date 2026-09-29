package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Creates {@link TechnicianProfile} instances.
 */
public class TechnicianProfileFactory implements UserProfileFactory {

  // inherited javadoc
  @Override
  public TechnicianProfile createProfile(Long userId, ProfileCreationData data) {
    Objects.requireNonNull(data, "data must not be null");
    return new TechnicianProfile(
        new UserId(userId),
        data.fullName(),
        data.email(),
        data.phone(),
        data.address(),
        Objects.requireNonNull(data.speciality(), "A technician profile requires a speciality"),
        data.certificationNumber());
  }
}
