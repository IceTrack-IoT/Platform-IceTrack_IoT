package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.ProfileCommandService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateUserProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.LinkProfileToUserCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.UserProfileFactory;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Profile Command Service Implementation
 */
@Service
@Slf4j
public class ProfileCommandServiceImpl implements ProfileCommandService {

  private static final String PROFILE_RESOURCE = "Profile";

  private final ProfileRepository profileRepository;

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
  public Result<Profile, ApplicationError> handle(CreateProfileCommand command) {
    try {
      var emailAddress = new EmailAddress(command.email());
      if (profileRepository.existsByEmailAddress(emailAddress)) {
        return Result.failure(ApplicationError.conflict(
            PROFILE_RESOURCE,
            "A profile with email address '%s' already exists".formatted(command.email())));
      }

      var profile = new Profile(command);
      var savedProfile = profileRepository.save(profile);
      return Result.success(savedProfile);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(PROFILE_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected(
          "Profile creation",
          e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Profile, ApplicationError> handle(CreateUserProfileCommand command) {
    try {
      var creationData = command.profileCreationData();

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

      // The role decides which profile is built; the caller never branches on it itself.
      var profileFactory = UserProfileFactory.forRole(creationData.role());
      var savedProfile = profileRepository.save(profileFactory.createFrom(creationData));
      log.info("Created {} profile {} for user {}",
          profileFactory.supportedRole(), savedProfile.getId(), creationData.userId().userId());
      return Result.success(savedProfile);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(PROFILE_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected(
          "User profile creation",
          e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<Profile, ApplicationError> handle(LinkProfileToUserCommand command) {
    var profile = profileRepository.findById(command.profileId());
    if (profile.isEmpty()) {
      return Result.failure(ApplicationError.notFound(PROFILE_RESOURCE, command.profileId().toString()));
    }
    try {
      var linkedProfile = profile.get();
      linkedProfile.linkToUser(command.userId());
      var savedProfile = profileRepository.save(linkedProfile);
      log.info("Linked profile {} to user {}", savedProfile.getId(), command.userId().userId());
      return Result.success(savedProfile);
    } catch (IllegalStateException e) {
      return Result.failure(ApplicationError.conflict(PROFILE_RESOURCE, e.getMessage()));
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(PROFILE_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Profile linking", e.getMessage()));
    }
  }
}
