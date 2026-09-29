package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.OwnerCommandService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.OwnerProfileFactory;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.OwnerProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Owner Command Service Implementation
 *
 * <p>An account is either an owner or a technician, never both, so the uniqueness checks run
 * against the {@link ProfileRepository}, which covers every role.</p>
 */
@Service
@Slf4j
public class OwnerCommandServiceImpl implements OwnerCommandService {

  private static final String OWNER_RESOURCE = "Owner";

  private final OwnerProfileRepository ownerProfileRepository;
  private final ProfileRepository profileRepository;
  private final OwnerProfileFactory ownerProfileFactory = new OwnerProfileFactory();

  /**
   * Constructor
   *
   * @param ownerProfileRepository The {@link OwnerProfileRepository} instance
   * @param profileRepository      The {@link ProfileRepository} instance
   */
  public OwnerCommandServiceImpl(OwnerProfileRepository ownerProfileRepository, ProfileRepository profileRepository) {
    this.ownerProfileRepository = ownerProfileRepository;
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<OwnerProfile, ApplicationError> handle(CreateOwnerCommand command) {
    try {
      if (profileRepository.existsByUserId(command.userId())) {
        return Result.failure(ApplicationError.conflict(
            OWNER_RESOURCE,
            "User '%s' already has a profile".formatted(command.userId().userId())));
      }
      if (profileRepository.existsByEmailAddress(command.data().email())) {
        return Result.failure(ApplicationError.conflict(
            OWNER_RESOURCE,
            "A profile with email address '%s' already exists".formatted(command.data().email().address())));
      }

      var owner = ownerProfileFactory.createProfile(command.userId().userId(), command.data());
      var savedOwner = ownerProfileRepository.save(owner);
      log.info("Created owner profile {} for user {}", savedOwner.getUserProfileId(), command.userId().userId());
      return Result.success(savedOwner);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(OWNER_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Owner creation", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<OwnerProfile, ApplicationError> handle(UpdateOwnerCommand command) {
    var existingOwner = ownerProfileRepository.findById(command.ownerId());
    if (existingOwner.isEmpty()) {
      return Result.failure(ApplicationError.notFound(OWNER_RESOURCE, command.ownerId().toString()));
    }
    try {
      var owner = existingOwner.get();
      owner.updateInfo(command.fullName(), command.phone(), command.address());
      owner.updateTaxRegistration(command.ruc());
      var savedOwner = ownerProfileRepository.save(owner);
      log.info("Updated owner profile {}", savedOwner.getUserProfileId());
      return Result.success(savedOwner);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(OWNER_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Owner update", e.getMessage()));
    }
  }
}
