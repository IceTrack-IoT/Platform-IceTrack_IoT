package pe.edu.upc.ice.track.platform.profiles.interfaces.acl;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;
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
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

/**
 * ACL facade that exposes Profiles bounded context capabilities to other contexts.
 *
 * <p>This class <em>is</em> the published contract of the Anti-Corruption Layer of the
 * {@code profiles} context. Every parameter its public methods accept and every value they return
 * is a primitive, a boxed primitive or a {@link String}: no {@code Profile} aggregate, value object,
 * factory, command, repository or JPA entity appears in its public signature. Callers depend on
 * this class alone and must never reach into {@code profiles.domain.*},
 * {@code profiles.application.*} or {@code profiles.infrastructure.*}.</p>
 *
 * <p>It is also the single place where values arriving from another bounded context are
 * translated into the profiles domain model: raw parameters are wrapped into value objects -
 * {@link UserId}, {@link PersonName}, {@link EmailAddress}, {@link Phone}, {@link StreetAddress},
 * {@link Ruc}, {@link Speciality} - each of which enforces its own invariants at construction
 * time, and assembled into a {@link ProfileCreationData}. A role specific command is then
 * dispatched to the owner or technician command service, which checks that neither the account
 * nor the email already has a profile, creates the aggregate through the matching
 * {@code UserProfileFactory} and persists it.</p>
 *
 * <p>Creation is role specific: there is one method per role, each taking exactly the attributes
 * that role needs, so no caller ever passes a value that belongs to another role.</p>
 *
 * <p>Failures are reported with JDK exceptions only, so the calling context never needs a
 * profiles type to understand them:</p>
 * <ul>
 *   <li>{@link IllegalArgumentException} - a supplied value violates a profiles invariant
 *       (malformed email, 10 digit RUC, blank speciality, incomplete address...);</li>
 *   <li>{@link IllegalStateException} - the account or the email already has a profile;</li>
 *   <li>any other {@link RuntimeException} - an unexpected failure.</li>
 * </ul>
 * <p>Each create method joins the caller's transaction: a registration and its profile commit or
 * roll back together.</p>
 */
@Service
@Slf4j
public class ProfilesContextFacade {

  private static final String CONFLICT_SUFFIX = "_CONFLICT";
  private static final String VALIDATION_ERROR = "VALIDATION_ERROR";

  private final OwnerCommandService ownerCommandService;
  private final TechnicianCommandService technicianCommandService;
  private final ProfileRepository profileRepository;

  public ProfilesContextFacade(
      OwnerCommandService ownerCommandService,
      TechnicianCommandService technicianCommandService,
      ProfileRepository profileRepository) {
    this.ownerCommandService = ownerCommandService;
    this.technicianCommandService = technicianCommandService;
    this.profileRepository = profileRepository;
  }

  /**
   * Creates the owner profile of a platform account.
   *
   * @param userId     identifier of the account the owner belongs to; required
   * @param fullName   display name of the account holder; required. It is split into a given and
   *                   a family name by the profiles context
   * @param email      email address of the account holder; required and well formed
   * @param phone      phone number of the account holder; required. A leading {@code +NN }
   *                   prefix separated by a space is read as the country code
   * @param street     street of the account holder's address; required
   * @param number     street number or apartment; may be {@code null}
   * @param city       city of the address; required
   * @param postalCode postal code of the address; required
   * @param country    country of the address; required
   * @param ruc        the owner's 11 digit taxpayer registration number; required
   * @return the identifier of the created profile, never {@code null}
   * @throws IllegalArgumentException when a value violates a profiles invariant
   * @throws IllegalStateException    when the account or the email already has a profile
   */
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

  /**
   * Creates the technician profile of a platform account.
   *
   * @param userId              identifier of the account the technician belongs to; required
   * @param fullName            display name of the account holder; required
   * @param email               email address of the account holder; required and well formed
   * @param phone               phone number of the account holder; required
   * @param street              street of the account holder's address; required
   * @param number              street number or apartment; may be {@code null}
   * @param city                city of the address; required
   * @param postalCode          postal code of the address; required
   * @param country             country of the address; required
   * @param speciality          the technician's speciality; required
   * @param certificationNumber the number of the technician's certification; required
   * @return the identifier of the created profile, never {@code null}
   * @throws IllegalArgumentException when a value violates a profiles invariant
   * @throws IllegalStateException    when the account or the email already has a profile
   */
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

  /**
   * Checks whether a profile of any role is bound to a platform account.
   *
   * @param userId identifier of the account
   * @return {@code true} when the account has a profile
   */
  @Transactional(readOnly = true)
  public boolean existsProfileByUserId(Long userId) {
    if (userId == null) return false;
    return profileRepository.existsByUserId(new UserId(userId));
  }

  /**
   * Fetches the email address of a platform account, as recorded on its profile.
   *
   * <p>The profiles context is the owner of the account holder's email address.</p>
   *
   * @param userId identifier of the account
   * @return the email address, or an empty string when the account has no profile
   */
  @Transactional(readOnly = true)
  public String fetchEmailByUserId(Long userId) {
    if (userId == null) return Strings.EMPTY;
    return profileRepository.findByUserId(new UserId(userId))
        .map(profile -> profile.getEmail().address())
        .orElse(Strings.EMPTY);
  }

  /**
   * Fetches the platform account whose profile uses an email address.
   *
   * @param email the email address to look up
   * @return the identifier of the account, or {@code 0L} when no profile uses the email address
   *         or the value is not a well formed email address
   */
  @Transactional(readOnly = true)
  public Long fetchUserIdByEmail(String email) {
    if (email == null || email.isBlank()) return 0L;
    EmailAddress emailAddress;
    try {
      emailAddress = new EmailAddress(email.trim());
    } catch (IllegalArgumentException exception) {
      // A malformed address cannot belong to any profile.
      return 0L;
    }
    return profileRepository.findByEmailAddress(emailAddress)
        .map(profile -> profile.getUserId().userId())
        .orElse(0L);
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
