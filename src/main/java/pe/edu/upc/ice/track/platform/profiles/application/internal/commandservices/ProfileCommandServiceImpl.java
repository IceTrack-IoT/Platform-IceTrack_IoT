package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.ProfileCommandService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.OwnerProfileFactory;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.TechnicianProfileFactory;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.function.Supplier;

/**
 * Profile Command Service Implementation
 */
@Service
@Slf4j
public class ProfileCommandServiceImpl implements ProfileCommandService {

  private static final String PROFILE_RESOURCE = "Profile";

  private final ProfileRepository profileRepository;
  private final OwnerProfileFactory ownerProfileFactory = new OwnerProfileFactory();
  private final TechnicianProfileFactory technicianProfileFactory = new TechnicianProfileFactory();

  /**
   * Constructor
   *
   * @param profileRepository The {@link ProfileRepository} instance
   */
  public ProfileCommandServiceImpl(ProfileRepository profileRepository) {
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Profile, ApplicationError> handle(CreateOwnerProfileCommand command) {
    return createProfile(
        command.profileCreationData(),
        () -> ownerProfileFactory.create(command.profileCreationData(), command.ruc()));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Profile, ApplicationError> handle(CreateTechnicianProfileCommand command) {
    return createProfile(
        command.profileCreationData(),
        () -> technicianProfileFactory.create(command.profileCreationData(), command.qualification()));
  }

  /**
   * Persists the profile built by a factory, once the account and the email are known to be free.
   *
   * @param creationData the shared creation data, used for the uniqueness checks
   * @param profileBuilder builds the concrete profile through the matching factory
   * @return the persisted profile, or the reason it could not be created
   */
  private Result<Profile, ApplicationError> createProfile(
      ProfileCreationData creationData, Supplier<? extends Profile> profileBuilder) {
    try {
      if (profileRepository.existsByUserId(creationData.userId())) {
        return Result.failure(ApplicationError.conflict(
            PROFILE_RESOURCE,
            "A profile for user '%s' already exists".formatted(creationData.userId().userId())));
      }
      if (profileRepository.existsByEmailAddress(creationData.email())) {
        return Result.failure(ApplicationError.conflict(
            PROFILE_RESOURCE,
            "A profile with email address '%s' already exists".formatted(creationData.email().address())));
      }

      var savedProfile = profileRepository.save(profileBuilder.get());
      log.info("Created {} profile {} for user {}",
          savedProfile.getRole(), savedProfile.getId(), creationData.userId().userId());
      return Result.success(savedProfile);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(PROFILE_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Profile creation", e.getMessage()));
    }
  }
}
