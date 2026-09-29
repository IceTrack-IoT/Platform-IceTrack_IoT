package pe.edu.upc.ice.track.platform.profiles.application.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.OwnerCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.TechnicianCommandService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.factories.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Speciality;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * Implementation of the {@link ProfilesContextFacade}.
 *
 * <p>The single place where values arriving from another bounded context are translated into
 * the profiles domain model: raw parameters are wrapped into value objects - {@link UserId},
 * {@link PersonName}, {@link EmailAddress}, {@link Phone}, {@link StreetAddress}, {@link Ruc},
 * {@link Speciality} - each of which enforces its own invariants at construction time, and
 * assembled into a {@link ProfileCreationData}. A role specific command is then dispatched to the
 * owner or technician command service, which checks that neither the account nor the email
 * already has a profile, creates the aggregate through the matching {@code UserProfileFactory}
 * and persists it.</p>
 */
@Service
@Slf4j
public class ProfilesContextFacadeImpl implements ProfilesContextFacade {

  private static final String CONFLICT_SUFFIX = "_CONFLICT";
  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

  private final OwnerCommandService ownerCommandService;
  private final TechnicianCommandService technicianCommandService;
  private final ProfileRepository profileRepository;

  public ProfilesContextFacadeImpl(
      OwnerCommandService ownerCommandService,
      TechnicianCommandService technicianCommandService,
      ProfileRepository profileRepository) {
    this.ownerCommandService = ownerCommandService;
    this.technicianCommandService = technicianCommandService;
    this.profileRepository = profileRepository;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Long createOwnerProfile(Long userId, String fullName, String email, String phone,
                                 String street, String number, String city, String postalCode, String country,
                                 Long ruc) {
    var data = ProfileCreationData.forOwner(
        PersonName.fromDisplayName(fullName),
        toEmailAddress(email),
        Phone.fromString(phone),
        new StreetAddress(street, number, city, postalCode, country),
        new Ruc(ruc));
    return toIdOrThrow(ownerCommandService.handle(new CreateOwnerCommand(toUserId(userId), data)));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Long createTechnicianProfile(Long userId, String fullName, String email, String phone,
                                      String street, String number, String city, String postalCode, String country,
                                      String speciality, String certificationNumber) {
    var data = ProfileCreationData.forTechnician(
        PersonName.fromDisplayName(fullName),
        toEmailAddress(email),
        Phone.fromString(phone),
        new StreetAddress(street, number, city, postalCode, country),
        new Speciality(speciality),
        certificationNumber);
    return toIdOrThrow(technicianCommandService.handle(new CreateTechnicianCommand(toUserId(userId), data)));
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public boolean existsProfileByUserId(Long userId) {
    if (userId == null) return false;
    return profileRepository.existsByUserId(new UserId(userId));
  }

  /**
   * Unwraps the identifier of a created profile, or reports the failure with a JDK exception
   * the calling context can understand without any profiles type.
   *
   * @param result the outcome of the create command
   * @param <T>    the concrete profile type
   * @return the identifier of the created profile
   */
  private static <T extends Profile> Long toIdOrThrow(Result<T, ApplicationError> result) {
    return switch (result) {
      case Result.Success<T, ApplicationError> success -> success.value().getUserProfileId();
      case Result.Failure<T, ApplicationError> failure -> throw toException(failure.error());
    };
  }

  private static RuntimeException toException(ApplicationError error) {
    var reason = error.details() == null ? error.message() : error.details();
    log.warn("The profiles context rejected a creation: {}", reason);
    if (error.code().endsWith(CONFLICT_SUFFIX)) {
      return new IllegalStateException(reason);
    }
    if (VALIDATION_ERROR.equals(error.code())) {
      return new IllegalArgumentException(reason);
    }
    return new RuntimeException(reason);
  }

  private static UserId toUserId(Long userId) {
    if (userId == null) {
      throw new IllegalArgumentException("A profile cannot be created without a user identifier");
    }
    return new UserId(userId);
  }

  private static EmailAddress toEmailAddress(String email) {
    if (email == null) {
      throw new IllegalArgumentException("Email address must not be null or blank");
    }
    return new EmailAddress(email.trim());
  }
}
