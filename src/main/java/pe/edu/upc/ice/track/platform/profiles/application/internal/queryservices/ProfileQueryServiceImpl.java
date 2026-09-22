package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.ProfileQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetAllProfilesQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByEmailQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;

import java.util.List;
import java.util.Optional;

/**
 * Application service that resolves Profiles bounded-context read queries.
 */
@Service
public class ProfileQueryServiceImpl implements ProfileQueryService {
  private final ProfileRepository profileRepository;

  /**
   * Creates the query service with the profile repository dependency.
   *
   * @param profileRepository profile repository port
   */
  public ProfileQueryServiceImpl(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  public Optional<Profile> handle(GetProfileByIdQuery query) {
    return profileRepository.findById(query.profileId());
  }

  // inherited javadoc
  @Override
  public Optional<Profile> handle(GetProfileByEmailQuery query) {
    return profileRepository.findByEmailAddress(query.email());
  }

  // inherited javadoc
  @Override
  public Optional<Profile> handle(GetProfileByUserIdQuery query) {
    return profileRepository.findByUserId(query.userId());
  }

  // inherited javadoc
  @Override
  public List<Profile> handle(GetAllProfilesQuery query) {
    return profileRepository.findAll();
  }
}
