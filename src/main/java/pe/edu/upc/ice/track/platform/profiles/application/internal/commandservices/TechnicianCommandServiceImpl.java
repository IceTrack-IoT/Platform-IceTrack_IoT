package pe.edu.upc.ice.track.platform.profiles.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.TechnicianCommandService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.TechnicianProfileFactory;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.TechnicianProfileRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Technician Command Service Implementation
 *
 * <p>An account is either an owner or a technician, never both, so the uniqueness checks run
 * against the {@link ProfileRepository}, which covers every role.</p>
 */
@Service
@Slf4j
public class TechnicianCommandServiceImpl implements TechnicianCommandService {

  private static final String TECHNICIAN_RESOURCE = "Technician";

  private final TechnicianProfileRepository technicianProfileRepository;
  private final ProfileRepository profileRepository;
  private final TechnicianProfileFactory technicianProfileFactory = new TechnicianProfileFactory();

  /**
   * Constructor
   *
   * @param technicianProfileRepository The {@link TechnicianProfileRepository} instance
   * @param profileRepository           The {@link ProfileRepository} instance
   */
  public TechnicianCommandServiceImpl(
      TechnicianProfileRepository technicianProfileRepository, ProfileRepository profileRepository) {
    this.technicianProfileRepository = technicianProfileRepository;
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<TechnicianProfile, ApplicationError> handle(CreateTechnicianCommand command) {
    try {
      if (profileRepository.existsByUserId(command.userId())) {
        return Result.failure(ApplicationError.conflict(
            TECHNICIAN_RESOURCE,
            "User '%s' already has a profile".formatted(command.userId().userId())));
      }
      if (profileRepository.existsByEmailAddress(command.data().email())) {
        return Result.failure(ApplicationError.conflict(
            TECHNICIAN_RESOURCE,
            "A profile with email address '%s' already exists".formatted(command.data().email().address())));
      }

      var technician = technicianProfileFactory.createProfile(command.userId().userId(), command.data());
      var savedTechnician = technicianProfileRepository.save(technician);
      log.info("Created technician profile {} for user {}",
          savedTechnician.getUserProfileId(), command.userId().userId());
      return Result.success(savedTechnician);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(TECHNICIAN_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Technician creation", e.getMessage()));
    }
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<TechnicianProfile, ApplicationError> handle(UpdateTechnicianCommand command) {
    var existingTechnician = technicianProfileRepository.findById(command.technicianId());
    if (existingTechnician.isEmpty()) {
      return Result.failure(ApplicationError.notFound(TECHNICIAN_RESOURCE, command.technicianId().toString()));
    }
    try {
      var technician = existingTechnician.get();
      technician.updateInfo(command.fullName(), command.phone(), command.address());
      technician.updateCertification(command.speciality(), command.certificationNumber());
      var savedTechnician = technicianProfileRepository.save(technician);
      log.info("Updated technician profile {}", savedTechnician.getUserProfileId());
      return Result.success(savedTechnician);
    } catch (IllegalArgumentException e) {
      return Result.failure(ApplicationError.validationError(TECHNICIAN_RESOURCE, e.getMessage()));
    } catch (Exception e) {
      return Result.failure(ApplicationError.unexpected("Technician update", e.getMessage()));
    }
  }
}
