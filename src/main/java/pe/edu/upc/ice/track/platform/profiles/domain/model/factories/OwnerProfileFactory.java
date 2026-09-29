package pe.edu.upc.ice.track.platform.profiles.domain.model.factories;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * Creates {@link OwnerProfile} instances.
 */
public class OwnerProfileFactory implements UserProfileFactory {

  // inherited javadoc
  @Override
  public OwnerProfile createProfile(Long userId, ProfileCreationData data) {
    Objects.requireNonNull(data, "data must not be null");
    return new OwnerProfile(
        new UserId(userId),
        data.fullName(),
        data.email(),
        data.phone(),
        data.address(),
        Objects.requireNonNull(data.ruc(), "An owner profile requires a RUC"));
  }
}
