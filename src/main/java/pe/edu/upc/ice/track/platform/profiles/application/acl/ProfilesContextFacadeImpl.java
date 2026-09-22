package pe.edu.upc.ice.track.platform.profiles.application.acl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.profiles.application.commandservices.ProfileCommandService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.ProfileQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateUserProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.LinkProfileToUserCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByEmailQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.EmailAddress;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.PersonName;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.Phone;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;
import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileRole;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Application-layer implementation of the Profiles ACL facade.
 *
 * <p>This class <em>is</em> the Anti-Corruption Layer of the {@code profiles} context. It is the
 * single place where values arriving from another bounded context are translated into the
 * profiles domain model:</p>
 * <ol>
 *   <li>raw parameters are wrapped into value objects - {@link UserId}, {@link PersonName},
 *       {@link EmailAddress}, {@link Phone}, {@link ProfileRole} - each of which enforces its own
 *       invariants at construction time;</li>
 *   <li>the result is bundled into a {@link ProfileCreationData};</li>
 *   <li>instantiation is delegated to the {@code UserProfileFactory} that matches the role, which
 *       the command service resolves, and the aggregate is persisted through the
 *       {@code ProfileRepository}.</li>
 * </ol>
 *
 * <p>Translation failures never escape: a malformed email or an unusable name is reported as
 * {@code 0L}, because the calling context has no vocabulary for a profiles domain exception.</p>
 */
@Service
@Slf4j
public class ProfilesContextFacadeImpl implements ProfilesContextFacade {

  /**
   * Returned whenever a profile could neither be found nor created.
   */
  private static final Long NO_PROFILE = 0L;

  private final ProfileCommandService profileCommandService;
  private final ProfileQueryService profileQueryService;

  public ProfilesContextFacadeImpl(ProfileCommandService profileCommandService, ProfileQueryService profileQueryService) {
    this.profileCommandService = profileCommandService;
    this.profileQueryService = profileQueryService;
  }

  // inherited javadoc
  @Override
  @Transactional
  public Long createProfile(Long userId, String fullName, String email, String phone, String role, String auxiliaryData) {
    if (userId == null) {
      log.warn("Refusing to create a profile without a user identifier");
      return NO_PROFILE;
    }

    // Idempotency: an identity context calls this on every sign-in, not only on registration.
    var existingProfileId = fetchExistingProfileId(userId, email);
    if (existingProfileId.isPresent()) {
      return existingProfileId.get();
    }

    ProfileCreationData creationData;
    try {
      creationData = toProfileCreationData(userId, fullName, email, phone, role, auxiliaryData);
    } catch (IllegalArgumentException | NullPointerException exception) {
      log.warn("Refusing to create the profile of user {}: {}", userId, exception.getMessage());
      return NO_PROFILE;
    }

    var result = profileCommandService.handle(new CreateUserProfileCommand(creationData));
    return result.toOptional()
        .map(Profile::getId)
        .orElseGet(() -> {
          log.warn("Could not create the profile of user {}", userId);
          return NO_PROFILE;
        });
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Long fetchProfileIdByUserId(Long userId) {
    if (userId == null) return NO_PROFILE;
    return profileQueryService.handle(new GetProfileByUserIdQuery(new UserId(userId)))
        .map(Profile::getId)
        .orElse(NO_PROFILE);
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Long fetchProfileIdByEmail(String email) {
    return toEmailAddressOrEmpty(email)
        .flatMap(emailAddress -> profileQueryService.handle(new GetProfileByEmailQuery(emailAddress)))
        .map(Profile::getId)
        .orElse(NO_PROFILE);
  }

  /**
   * Looks for a profile that already covers this registration, by account first and by email
   * second.
   *
   * <p>A match on the account is returned as is. A match on the email means the profile was
   * created before its owner registered, so it is bound to the account before being returned -
   * without that link the email lookup would have to be repeated on every sign-in, and the
   * profile would stay ownerless. A profile already owned by a different account is never
   * handed over; it is reported as absent so the caller fails instead of adopting it.</p>
   *
   * @param userId identifier of the account
   * @param email  email address of the account holder, may be {@code null}
   * @return the existing profile identifier, or empty when the account has no profile yet
   */
  private Optional<Long> fetchExistingProfileId(Long userId, String email) {
    var accountUserId = new UserId(userId);
    var profileByUserId = profileQueryService.handle(new GetProfileByUserIdQuery(accountUserId));
    if (profileByUserId.isPresent()) {
      return profileByUserId.map(Profile::getId);
    }

    var profileByEmail = toEmailAddressOrEmpty(email)
        .flatMap(emailAddress -> profileQueryService.handle(new GetProfileByEmailQuery(emailAddress)));
    if (profileByEmail.isEmpty()) {
      return Optional.empty();
    }

    var linkResult = profileCommandService.handle(
        new LinkProfileToUserCommand(profileByEmail.get().getId(), accountUserId));
    if (linkResult.isFailure()) {
      log.warn("A profile already registered under the email of user {} could not be linked to it", userId);
      return Optional.empty();
    }
    return linkResult.toOptional().map(Profile::getId);
  }

  /**
   * Translates the agnostic parameters of the facade into the profiles domain model.
   *
   * @param userId        identifier of the account the profile belongs to
   * @param fullName      display name of the account holder
   * @param email         email address of the account holder
   * @param phone         phone number of the account holder, may be {@code null}
   * @param role          role name supplied by the calling context
   * @param auxiliaryData optional opaque annotation
   * @return the validated creation data
   * @throws IllegalArgumentException when a value object constraint is violated
   */
  private ProfileCreationData toProfileCreationData(
      Long userId, String fullName, String email, String phone, String role, String auxiliaryData) {
    return new ProfileCreationData(
        new UserId(userId),
        PersonName.fromDisplayName(fullName),
        new EmailAddress(email),
        ProfileRole.fromRoleName(role),
        toPhoneOrNull(phone),
        null,
        auxiliaryData);
  }

  /**
   * Builds an email address, reporting a malformed value as an empty result instead of an
   * exception, so that a lookup never breaks the calling context.
   *
   * @param email the raw email address
   * @return the email address, or empty when it is missing or malformed
   */
  private Optional<EmailAddress> toEmailAddressOrEmpty(String email) {
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
   * @param phone the raw phone number, may be {@code null}
   * @return the phone number, or {@code null} when none was supplied
   */
  private static Phone toPhoneOrNull(String phone) {
    if (phone == null || phone.isBlank()) return null;
    var trimmed = phone.trim();
    var separatorIndex = trimmed.indexOf(' ');
    if (trimmed.startsWith("+") && separatorIndex > 1 && separatorIndex < trimmed.length() - 1) {
      return new Phone(trimmed.substring(0, separatorIndex), trimmed.substring(separatorIndex + 1).trim());
    }
    return Phone.withoutCountryCode(trimmed);
  }
}
