package pe.edu.upc.ice.track.platform.profiles.interfaces.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.ProfileCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.ProfileQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByEmailQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Ruc;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.StreetAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianQualification;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * ACL facade that exposes Profiles bounded context capabilities to other contexts.
 *
 * <p>This class <em>is</em> the Anti-Corruption Layer of the {@code profiles} context. Every
 * parameter it accepts and every value it returns is a primitive, a boxed primitive or a
 * {@link String}: no profiles aggregate, value object, command, repository or JPA entity appears
 * in its public signature, and no foreign type is accepted either. Callers depend on this class
 * alone and must never reach into {@code profiles.domain.*}, {@code profiles.application.*} or
 * {@code profiles.infrastructure.*}.</p>
 *
 * <p>Inside, it is the single place where values arriving from another bounded context are
 * translated into the profiles domain model:</p>
 * <ol>
 *   <li>raw parameters are wrapped into value objects - {@link UserId}, {@link PersonName},
 *       {@link EmailAddress}, {@link Phone}, {@link StreetAddress}, {@link Ruc},
 *       {@link TechnicianQualification} - each of which enforces its own invariants at
 *       construction time;</li>
 *   <li>the shared attributes are bundled into a {@link ProfileCreationData};</li>
 *   <li>a role specific command is dispatched to the {@link ProfileCommandService}, which builds
 *       the concrete profile through the {@code OwnerProfileFactory} or the
 *       {@code TechnicianProfileFactory} and persists it through the {@code ProfileRepository}.</li>
 * </ol>
 *
 * <p>Failures are reported with JDK exceptions only, so the calling context never needs a
 * profiles type to understand them:</p>
 * <ul>
 *   <li>{@link IllegalArgumentException} - a supplied value violates a profiles invariant
 *       (malformed email, 10 digit RUC, blank speciality, incomplete address...);</li>
 *   <li>{@link IllegalStateException} - the profile conflicts with an existing one (the account
 *       or the email already has a profile);</li>
 *   <li>any other {@link RuntimeException} - an unexpected failure.</li>
 * </ul>
 * <p>Each create method is {@code @Transactional} with the default propagation, so it joins the
 * caller's transaction: a registration and its profile commit or roll back together.</p>
 */
@Service
@Slf4j
public class ProfilesContextFacade {

  /**
   * Returned by the lookups whenever no profile matches.
   */
  private static final Long NO_PROFILE = 0L;

  private static final String CONFLICT_SUFFIX = "_CONFLICT";

  private final ProfileCommandService profileCommandService;
  private final ProfileQueryService profileQueryService;

  public ProfilesContextFacade(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService) {
    this.profileCommandService = profileCommandService;
    this.profileQueryService = profileQueryService;
  }

  /**
   * Creates the {@code OwnerProfile} of a platform account.
   *
   * @param userId     identifier of the account the profile belongs to; required
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
    var creationData = toProfileCreationData(userId, fullName, email, phone, street, number, city, postalCode, country);
    var command = new CreateOwnerProfileCommand(creationData, new Ruc(ruc));
    return toProfileIdOrThrow(profileCommandService.handle(command));
  }

  /**
   * Creates the {@code TechnicianProfile} of a platform account.
   *
   * @param userId              identifier of the account the profile belongs to; required
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
    var creationData = toProfileCreationData(userId, fullName, email, phone, street, number, city, postalCode, country);
    var command = new CreateTechnicianProfileCommand(
        creationData,
        new TechnicianQualification(speciality, certificationNumber));
    return toProfileIdOrThrow(profileCommandService.handle(command));
  }

  /**
   * Fetches the identifier of the profile linked to a platform account.
   *
   * @param userId identifier of the account
   * @return profile identifier, or {@code 0L} when not found
   */
  @Transactional(readOnly = true)
  public Long fetchProfileIdByUserId(Long userId) {
    if (userId == null) return NO_PROFILE;
    return profileQueryService.handle(new GetProfileByUserIdQuery(new UserId(userId)))
        .map(Profile::getId)
        .orElse(NO_PROFILE);
  }

  /**
   * Fetches a profile identifier by email.
   *
   * @param email profile email address
   * @return profile identifier, or {@code 0L} when not found or when the email is malformed
   */
  @Transactional(readOnly = true)
  public Long fetchProfileIdByEmail(String email) {
    return toEmailAddressOrEmpty(email)
        .flatMap(emailAddress -> profileQueryService.handle(new GetProfileByEmailQuery(emailAddress)))
        .map(Profile::getId)
        .orElse(NO_PROFILE);
  }

  /**
   * Translates the agnostic parameters shared by every profile into the profiles domain model.
   *
   * @return the validated creation data
   * @throws IllegalArgumentException when a value object constraint is violated
   */
  private static ProfileCreationData toProfileCreationData(
      Long userId, String fullName, String email, String phone,
      String street, String number, String city, String postalCode, String country) {
    if (userId == null) {
      throw new IllegalArgumentException("A profile cannot be created without a user identifier");
    }
    if (email == null) {
      throw new IllegalArgumentException("Email address must not be null or blank");
    }
    return new ProfileCreationData(
        new UserId(userId),
        PersonName.fromDisplayName(fullName),
        new EmailAddress(email.trim()),
        toPhone(phone),
        new StreetAddress(trimOrNull(street), trimOrNull(number), trimOrNull(city), trimOrNull(postalCode), trimOrNull(country)));
  }

  /**
   * Unwraps the identifier of a created profile, or reports the failure with a JDK exception the
   * calling context can understand without any profiles type.
   *
   * @param result the outcome of the create command
   * @return the identifier of the created profile
   * @throws IllegalStateException    when the profile conflicts with an existing one
   * @throws IllegalArgumentException when the profile violates a profiles invariant
   * @throws RuntimeException         when the profile could not be created for any other reason
   */
  private static Long toProfileIdOrThrow(Result<Profile, ApplicationError> result) {
    return switch (result) {
      case Result.Success<Profile, ApplicationError> success -> success.value().getId();
      case Result.Failure<Profile, ApplicationError> failure -> throw toException(failure.error());
    };
  }

  private static RuntimeException toException(ApplicationError error) {
    var reason = error.details() == null ? error.message() : error.details();
    log.warn("The profiles context rejected a profile creation: {}", reason);
    if (error.code().endsWith(CONFLICT_SUFFIX)) {
      return new IllegalStateException(reason);
    }
    if ("VALIDATION_ERROR".equals(error.code())) {
      return new IllegalArgumentException(reason);
    }
    return new RuntimeException(reason);
  }

  /**
   * Builds an email address, reporting a malformed value as an empty result instead of an
   * exception, so that a lookup never breaks the calling context.
   *
   * @param email the raw email address
   * @return the email address, or empty when it is missing or malformed
   */
  private static Optional<EmailAddress> toEmailAddressOrEmpty(String email) {
    if (email == null || email.isBlank()) return Optional.empty();
    try {
      return Optional.of(new EmailAddress(email.trim()));
    } catch (IllegalArgumentException exception) {
      log.warn("Ignoring a malformed email address in a profiles ACL lookup");
      return Optional.empty();
    }
  }

  /**
   * Builds a phone number out of a single raw value.
   *
   * <p>The facade receives the phone as one string because calling contexts do not model a
   * country code. A leading {@code +NN} prefix is split off when present, otherwise the whole
   * value is kept as the national number with an unknown country code.</p>
   *
   * @param phone the raw phone number; required
   * @return the phone number
   * @throws IllegalArgumentException when the phone number is missing
   */
  private static Phone toPhone(String phone) {
    if (phone == null || phone.isBlank()) {
      throw new IllegalArgumentException("Phone number must not be null or blank");
    }
    var trimmed = phone.trim();
    var separatorIndex = trimmed.indexOf(' ');
    if (trimmed.startsWith("+") && separatorIndex > 1 && separatorIndex < trimmed.length() - 1) {
      return new Phone(trimmed.substring(0, separatorIndex), trimmed.substring(separatorIndex + 1).trim());
    }
    return Phone.withoutCountryCode(trimmed);
  }

  private static String trimOrNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}
