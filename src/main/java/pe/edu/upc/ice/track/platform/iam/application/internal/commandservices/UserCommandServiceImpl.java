package pe.edu.upc.ice.track.platform.iam.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleTokenService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleUserInfo;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.hashing.HashingService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.profiles.ExternalProfileService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens.TokenService;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.ExchangeGoogleTokenCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * User command service implementation.
 *
 * <p>Owns the two authentication flows of the IAM bounded context:</p>
 * <ul>
 *   <li><strong>Local</strong> - username and password verified against the hashing service.</li>
 *   <li><strong>Google Token Exchange</strong> - the frontend performs the Google login and
 *       submits the resulting OIDC id_token; the token is validated through the
 *       {@link GoogleTokenService} port (backed by Spring Security's {@code NimbusJwtDecoder})
 *       and exchanged for the platform's own bearer token.</li>
 * </ul>
 *
 * <p>Both flows provision the matching profile through the {@link ExternalProfileService}
 * outbound port, which reaches the {@code profiles} context across its ACL facade. The call
 * happens inside this service's transaction, so an account and its profile are committed
 * together or not at all. It is also made on <em>every</em> successful authentication rather
 * than only on registration: the port is idempotent, which keeps repeated sign-ins free of
 * duplicates while back-filling accounts that predate the profile integration.</p>
 */
@Service
@Slf4j
public class UserCommandServiceImpl implements UserCommandService {

  private static final String USER_RESOURCE = "User";
  private static final String ROLE_NAME_SUFFIX = "_ROLE";

  private final UserRepository userRepository;
  private final HashingService hashingService;
  private final TokenService tokenService;
  private final RoleRepository roleRepository;
  private final GoogleTokenService googleTokenService;
  private final ExternalProfileService externalProfileService;

  public UserCommandServiceImpl(
      UserRepository userRepository,
      HashingService hashingService,
      TokenService tokenService,
      RoleRepository roleRepository,
      GoogleTokenService googleTokenService,
      ExternalProfileService externalProfileService) {
    this.userRepository = userRepository;
    this.hashingService = hashingService;
    this.tokenService = tokenService;
    this.roleRepository = roleRepository;
    this.googleTokenService = googleTokenService;
    this.externalProfileService = externalProfileService;
  }

  // inherited javadoc
  @Override
  @Transactional(readOnly = true)
  public Result<ImmutablePair<User, String>, ApplicationError> handle(SignInByLocalCommand command) {
    if (command == null || command.username() == null || command.username().isBlank()) {
      return Result.failure(ApplicationError.validationError("username", "Username must not be null or blank"));
    }
    var user = userRepository.findByUsername(command.username());
    if (user.isEmpty()) {
      return Result.failure(ApplicationError.notFound(USER_RESOURCE, command.username()));
    }
    var foundUser = user.get();
    if (foundUser.isFederated()) {
      return Result.failure(ApplicationError.businessRuleViolation(
          "local-credentials",
          "This account is federated through %s and must sign in with that provider"
              .formatted(foundUser.getProvider())));
    }
    var rawPassword = command.password() == null ? "" : command.password();
    var encodedPassword = foundUser.getPassword() == null ? "" : foundUser.getPassword();
    if (encodedPassword.isBlank() || !hashingService.matches(rawPassword, encodedPassword)) {
      return Result.failure(ApplicationError.validationError("credentials", "Invalid username or password"));
    }
    return Result.success(ImmutablePair.of(foundUser, tokenService.generateToken(foundUser.getUsername())));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<User, ApplicationError> handle(SignUpByLocalCommand command) {
    if (command == null || command.username() == null || command.username().isBlank()) {
      return Result.failure(ApplicationError.validationError("username", "Username must not be null or blank"));
    }
    if (command.password() == null || command.password().isBlank()) {
      return Result.failure(ApplicationError.validationError("password", "Password must not be null or blank"));
    }
    // The email is the profile's identity in the profiles context, so an account cannot be
    // registered without one: rejecting it here keeps the failure a clean 400 instead of an
    // account that no profile can be attached to.
    if (command.email() == null || command.email().isBlank()) {
      return Result.failure(ApplicationError.validationError("email", "Email must not be null or blank"));
    }
    if (userRepository.existsByUsername(command.username())) {
      return Result.failure(ApplicationError.conflict(
          USER_RESOURCE,
          "A user with username %s already exists".formatted(command.username())));
    }
    if (userRepository.existsByEmail(command.email())) {
      return Result.failure(ApplicationError.conflict(
          USER_RESOURCE,
          "A user with email %s already exists".formatted(command.email())));
    }

    var roles = toPersistedRoles(command.role());
    var user = new User(
        command.username(),
        hashingService.encode(command.password()),
        command.email(),
        roles);
    var savedUser = userRepository.save(user);
    requireProfileFor(savedUser, savedUser.getUsername(), null);
    return Result.success(savedUser);
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ImmutablePair<User, String>, ApplicationError> handle(ExchangeGoogleTokenCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Token exchange command must not be null"));
    }

    GoogleUserInfo googleUserInfo;
    try {
      googleUserInfo = googleTokenService.verify(command.idToken());
    } catch (IllegalArgumentException exception) {
      return Result.failure(ApplicationError.validationError("idToken", exception.getMessage()));
    }

    var existingUser = resolveExistingUser(googleUserInfo);
    if (existingUser.isPresent()) {
      var user = existingUser.get();
      // An account created locally and now signing in with the same Google email keeps its
      // identifier, credentials and roles; it simply starts accepting the federated provider.
      if (!googleUserInfo.subject().equals(user.getExternalId())) {
        user.linkGoogleAccount(googleUserInfo.subject());
        user = userRepository.save(user);
      }
      // Idempotent by contract: this retrieves the existing profile rather than creating one,
      // and back-fills accounts registered before the profiles integration existed.
      ensureProfileFor(user, googleUserInfo.displayName(), googleUserInfo.pictureUrl());
      return Result.success(ImmutablePair.of(user, tokenService.generateToken(user.getUsername())));
    }

    if (userRepository.existsByUsername(googleUserInfo.email())) {
      return Result.failure(ApplicationError.conflict(
          USER_RESOURCE,
          "A user with username %s already exists".formatted(googleUserInfo.email())));
    }

    var roles = toPersistedRoles(toRequestedRoles(command));
    var registeredUser = userRepository.save(
        User.registeredWithGoogle(googleUserInfo.email(), googleUserInfo.subject(), roles));
    requireProfileFor(registeredUser, googleUserInfo.displayName(), googleUserInfo.pictureUrl());
    log.info("Registered user {} through the Google token exchange", registeredUser.getId());

    return Result.success(
        ImmutablePair.of(registeredUser, tokenService.generateToken(registeredUser.getUsername())));
  }

  /**
   * Provisions the profile of an account that has just been registered.
   *
   * <p>The call runs inside the current transaction, so an account and its profile commit
   * together: when the profiles context cannot provision one, the exception propagates out of
   * this {@code @Transactional} method and rolls the registration back rather than leaving an
   * account nobody can build a profile for.</p>
   *
   * @param user          the persisted account, carrying its assigned identifier
   * @param fullName      display name to hand over to the profiles context
   * @param auxiliaryData optional annotation released by the identity provider, may be {@code null}
   * @throws IllegalStateException when no profile could be provisioned
   */
  private void requireProfileFor(User user, String fullName, String auxiliaryData) {
    if (fetchOrCreateProfileFor(user, fullName, auxiliaryData).isEmpty()) {
      throw new IllegalStateException(
          "The profile of user %s could not be provisioned".formatted(user.getId()));
    }
  }

  /**
   * Back-fills the profile of an account that already exists.
   *
   * <p>Unlike {@link #requireProfileFor}, a failure here is logged and tolerated: the account was
   * registered long ago and is authenticating right now, so a profile anomaly must not turn a
   * valid sign-in into an error. The profile can still be repaired through the profiles API.</p>
   *
   * @param user          the authenticated account
   * @param fullName      display name to hand over to the profiles context
   * @param auxiliaryData optional annotation released by the identity provider, may be {@code null}
   */
  private void ensureProfileFor(User user, String fullName, String auxiliaryData) {
    if (fetchOrCreateProfileFor(user, fullName, auxiliaryData).isEmpty()) {
      log.warn("User {} signed in without a provisioned profile", user.getId());
    }
  }

  /**
   * Reaches the {@code profiles} bounded context through the outbound ACL port.
   *
   * <p>Only agnostic values cross this call: identifiers, names and role names. The port is
   * idempotent, so invoking it on every authentication retrieves the existing profile instead of
   * duplicating it.</p>
   *
   * @param user          the account whose profile is required
   * @param fullName      display name to hand over to the profiles context
   * @param auxiliaryData optional annotation released by the identity provider, may be {@code null}
   * @return the profile identifier, or empty when none could be found or created
   */
  private Optional<Long> fetchOrCreateProfileFor(User user, String fullName, String auxiliaryData) {
    return externalProfileService.fetchOrCreateProfile(
        user.getId(),
        fullName,
        user.getEmail(),
        null,
        user.getPrimaryRoleName(),
        auxiliaryData);
  }

  /**
   * Resolves the platform account matching a verified Google identity.
   *
   * <p>The Google {@code sub} claim drives the primary lookup because it is stable even when the
   * account owner changes the email. The email is used as a fallback so that an account created
   * locally is linked instead of duplicated.</p>
   *
   * @param googleUserInfo the verified Google claims
   * @return the matching user, or empty when the Google account is unknown
   */
  private Optional<User> resolveExistingUser(GoogleUserInfo googleUserInfo) {
    var bySubject = userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, googleUserInfo.subject());
    if (bySubject.isPresent()) {
      return bySubject;
    }
    return userRepository.findByEmail(googleUserInfo.email());
  }

  /**
   * Translates the role requested at exchange time into IAM domain roles.
   *
   * <p>Both the canonical name ({@code OWNER_ROLE}) and its bare form ({@code OWNER}) are
   * accepted, in any case. An unknown name is ignored rather than rejected, so that a stale
   * frontend never blocks a sign-in: the account is registered with the default role instead.</p>
   *
   * @param command the token exchange command
   * @return the requested roles, or an empty list when none was requested or the name is unknown
   */
  private List<Role> toRequestedRoles(ExchangeGoogleTokenCommand command) {
    if (!command.hasRequestedRole()) {
      return List.of();
    }
    var roleName = command.requestedRole().trim().toUpperCase(Locale.ROOT);
    if (!roleName.endsWith(ROLE_NAME_SUFFIX)) {
      roleName = roleName + ROLE_NAME_SUFFIX;
    }
    try {
      return List.of(new Role(Roles.valueOf(roleName)));
    } catch (IllegalArgumentException exception) {
      log.warn("Ignoring unknown requested role {} during the Google token exchange", command.requestedRole());
      return List.of();
    }
  }

  /**
   * Replaces the supplied roles with their persisted counterparts, so that the user is linked to
   * the seeded role rows instead of creating detached duplicates.
   *
   * @param roles the roles to resolve; a {@code null} or empty list resolves to the default role
   * @return the persisted roles, never empty
   */
  private List<Role> toPersistedRoles(List<Role> roles) {
    var requestedRoles = Role.validateRoleSet(roles);
    return requestedRoles.stream()
        .filter(role -> role != null && role.getName() != null)
        .map(role -> roleRepository.findByName(role.getName()).orElseGet(() -> roleRepository.save(role)))
        .toList();
  }
}
