package pe.edu.upc.ice.track.platform.iam.application.internal.commandservices;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import pe.edu.upc.ice.track.platform.iam.application.commandservices.UserCommandService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl.ExternalProfileService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleTokenService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleUserInfo;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.hashing.HashingService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens.RefreshTokenService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens.TokenService;
import pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleOwnerRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.CompleteGoogleTechnicianRegistrationCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.PurgeExpiredRefreshTokensCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.RefreshTokenCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByGoogleCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignInByLocalCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignOutCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpOwnerCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.SignUpTechnicianCommand;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.RefreshToken;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthErrorCode;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.AuthProvider;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.SessionTokens;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RefreshTokenRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;

/**
 * User command service implementation.
 *
 * <p>Owns the authentication and registration flows of the IAM bounded context:</p>
 * <ul>
 *   <li><strong>Local</strong> - role explicit sign-up ({@link SignUpOwnerCommand},
 *       {@link SignUpTechnicianCommand}); sign-in verified against the hashing service.</li>
 *   <li><strong>Google, deferred registration</strong> - the frontend performs the Google login
 *       and submits the OIDC id_token. {@link SignInByGoogleCommand} signs a known account in, or
 *       reports that the onboarding is required without writing anything. The role explicit
 *       {@link CompleteGoogleOwnerRegistrationCommand} or
 *       {@link CompleteGoogleTechnicianRegistrationCommand} then creates the account. Tokens are
 *       validated statelessly through the {@link GoogleTokenService} port, backed by Spring
 *       Security's {@code NimbusJwtDecoder}.</li>
 * </ul>
 *
 * <p>The role of a new account is fixed by the command it is created from - owner commands
 * assign {@link Roles#OWNER_ROLE} and provision an owner profile, technician commands assign
 * {@link Roles#TECHNICIAN_ROLE} and provision a technician profile. Every registration creates the
 * account and its profile through the {@link ExternalProfileService} outbound service inside one
 * transaction. When the profiles context rejects the profile, the transaction is marked
 * rollback-only before the failure is returned, so no account can ever exist without its profile,
 * and no profile without its account.</p>
 *
 * <p>It also owns the session lifecycle. Every sign-in opens a session made of a short-lived
 * access JWT and an opaque refresh token persisted as its digest ({@link RefreshTokenCommand}
 * rotates it, {@link SignOutCommand} revokes it). An exchanged refresh token is kept as rotated
 * rather than deleted, so that replaying it is detected and revokes every session of the
 * account; one presented again within a short grace window after its rotation is only rejected,
 * since that is a concurrent refresh by the legitimate client. Every rejection is a
 * {@link RefreshTokenException} carrying a typed {@link AuthErrorCode}. Expired tokens are removed
 * by {@link PurgeExpiredRefreshTokensCommand}.</p>
 */
@Service
@Slf4j
public class UserCommandServiceImpl implements UserCommandService {

  private static final String USER_RESOURCE = "User";
  private static final String PROFILE_RESOURCE = "Profile";

  private final UserRepository userRepository;
  private final HashingService hashingService;
  private final TokenService tokenService;
  private final RoleRepository roleRepository;
  private final GoogleTokenService googleTokenService;
  private final ExternalProfileService externalProfileService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final RefreshTokenService refreshTokenService;

  public UserCommandServiceImpl(
      UserRepository userRepository,
      HashingService hashingService,
      TokenService tokenService,
      RoleRepository roleRepository,
      GoogleTokenService googleTokenService,
      ExternalProfileService externalProfileService,
      RefreshTokenRepository refreshTokenRepository,
      RefreshTokenService refreshTokenService) {
    this.userRepository = userRepository;
    this.hashingService = hashingService;
    this.tokenService = tokenService;
    this.roleRepository = roleRepository;
    this.googleTokenService = googleTokenService;
    this.externalProfileService = externalProfileService;
    this.refreshTokenRepository = refreshTokenRepository;
    this.refreshTokenService = refreshTokenService;
  }

  // inherited javadoc
  @Override
  @Transactional // not read-only: a successful sign-in persists the refresh token of the new session
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(SignInByLocalCommand command) {
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
    return Result.success(authenticate(foundUser));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<User, ApplicationError> handle(SignUpOwnerCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Owner sign-up command must not be null"));
    }
    return signUpLocally(
        command.username(),
        command.password(),
        command.email(),
        Roles.OWNER_ROLE,
        user -> createOwnerProfile(user, command.fullName(), command.email(), command.contactDetails(), command.ruc()));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<User, ApplicationError> handle(SignUpTechnicianCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Technician sign-up command must not be null"));
    }
    return signUpLocally(
        command.username(),
        command.password(),
        command.email(),
        Roles.TECHNICIAN_ROLE,
        user -> createTechnicianProfile(
            user,
            command.fullName(),
            command.email(),
            command.contactDetails(),
            command.speciality(),
            command.certificationNumber()));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(SignInByGoogleCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Google sign-in command must not be null"));
    }
    return verifyGoogleIdToken(command.idToken()).flatMap(googleUserInfo -> {
      var existingUser = resolveExistingUser(googleUserInfo);
      if (existingUser.isEmpty()) {
        // Deferred registration: nothing is persisted until the onboarding form is completed.
        return Result.failure(onboardingRequired());
      }
      var user = linkGoogleAccountIfNeeded(existingUser.get(), googleUserInfo);
      return Result.success(authenticate(user));
    });
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(CompleteGoogleOwnerRegistrationCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Google owner registration command must not be null"));
    }
    return verifyGoogleIdToken(command.idToken()).flatMap(googleUserInfo -> completeGoogleRegistration(
        googleUserInfo,
        command.username(),
        Roles.OWNER_ROLE,
        user -> createOwnerProfile(
            user, googleUserInfo.displayName(), googleUserInfo.email(), command.contactDetails(), command.ruc())));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(CompleteGoogleTechnicianRegistrationCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Google technician registration command must not be null"));
    }
    return verifyGoogleIdToken(command.idToken()).flatMap(googleUserInfo -> completeGoogleRegistration(
        googleUserInfo,
        command.username(),
        Roles.TECHNICIAN_ROLE,
        user -> createTechnicianProfile(
            user,
            googleUserInfo.displayName(),
            googleUserInfo.email(),
            command.contactDetails(),
            command.speciality(),
            command.certificationNumber())));
  }

  // inherited javadoc
  // A RefreshTokenException must not roll back: the replay mitigation revokes every session of
  // the account and then throws, and that revocation has to be committed.
  @Override
  @Transactional(noRollbackFor = RefreshTokenException.class)
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(RefreshTokenCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Refresh token command must not be null"));
    }
    var refreshToken = refreshTokenRepository.findByToken(refreshTokenService.hashToken(command.refreshToken()))
        .orElseThrow(() -> new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_INVALID));
    if (refreshToken.isExpired()) {
      throw new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    }
    if (refreshToken.isRevoked()) {
      throw rejectRevokedToken(refreshToken);
    }
    // Resolved before rotating, so that a token of a deleted account is not consumed for nothing.
    // The access token is minted from the reloaded account, so it always carries its current role.
    var user = userRepository.findById(refreshToken.getUserId())
        .orElseThrow(() -> new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_INVALID));

    var newRawToken = refreshTokenService.generateToken();
    refreshToken.rotate(refreshTokenService.hashToken(newRawToken));
    // Atomic compare-and-set: when the same token is presented concurrently, only one request
    // rotates it. Losing the race means another request rotated it a few milliseconds ago, which
    // is the concurrent refresh case, never a replay.
    if (!refreshTokenRepository.saveRotation(refreshToken)) {
      throw new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_RECENTLY_ROTATED);
    }
    return Result.success(openSession(user, newRawToken));
  }

  // inherited javadoc
  @Override
  @Transactional
  public void handle(SignOutCommand command) {
    if (command == null) {
      return;
    }
    // Revoked without a replacement: presenting the token later is answered with
    // REFRESH_TOKEN_REVOKED, and is never mistaken for a replay of a rotated token.
    refreshTokenRepository.revoke(refreshTokenService.hashToken(command.refreshToken()));
  }

  // inherited javadoc
  @Override
  @Transactional
  public int handle(PurgeExpiredRefreshTokensCommand command) {
    var purged = refreshTokenRepository.deleteAllExpiredBefore(Instant.now());
    log.info("Purged {} expired refresh tokens", purged);
    return purged;
  }

  /**
   * Registers an account with local credentials and the role implied by the calling command.
   *
   * @param username           the unique username
   * @param password           the raw password, hashed before it reaches the aggregate
   * @param email              the email address; owned by the profiles context, which stores it on
   *                            the profile, so its uniqueness is checked there
   * @param role               the definitive role implied by the command
   * @param profileProvisioner creates the profile matching {@code role} for the persisted account
   * @return the persisted account, or the reason the registration was rejected
   */
  private Result<User, ApplicationError> signUpLocally(
      String username, String password, String email, Roles role, Function<User, Long> profileProvisioner) {
    if (userRepository.existsByUsername(username)) {
      return Result.failure(ApplicationError.conflict(
          USER_RESOURCE,
          "A user with username %s already exists".formatted(username)));
    }
    // Checked up front to keep the USER_CONFLICT answer; the profiles context would otherwise only
    // reject the duplicate after the account insert, as a PROFILE_CONFLICT.
    if (externalProfileService.fetchUserIdByEmail(email).isPresent()) {
      return Result.failure(ApplicationError.conflict(
          USER_RESOURCE,
          "A user with email %s already exists".formatted(email)));
    }
    var user = User.registeredLocally(username, hashingService.encode(password), toPersistedRole(role));
    return registerWithProfile(user, profileProvisioner);
  }

  /**
   * Registers the Google account described by verified claims with the role implied by the
   * calling command, or signs it in when it already exists.
   *
   * <p>The {@code desiredUsername} comes from the onboarding form (e.g. owner flow). The
   * Google email is never used as username; it is only stored on the profile as contact
   * data.</p>
   *
   * @param googleUserInfo     the verified Google claims
   * @param desiredUsername    the username requested in the onboarding form
   * @param role               the definitive role implied by the command
   * @param profileProvisioner creates the profile matching {@code role} for the persisted account
   * @return the authenticated user with its session tokens, or the reason the registration failed
   */
  private Result<ImmutablePair<User, SessionTokens>, ApplicationError> completeGoogleRegistration(
      GoogleUserInfo googleUserInfo, String desiredUsername, Roles role, Function<User, Long> profileProvisioner) {
    var existingUser = resolveExistingUser(googleUserInfo);
    if (existingUser.isPresent()) {
      // A repeated submission, or an account registered meanwhile: the role is immutable, so the
      // account signs in with the role it already has and the submitted form is ignored.
      var user = linkGoogleAccountIfNeeded(existingUser.get(), googleUserInfo);
      if (!user.hasRole(role)) {
        log.warn("User {} completed a Google registration as {} but already holds {}; keeping the existing role",
            user.getId(), role, user.getRoleName());
      }
      return Result.success(authenticate(user));
    }

    if (desiredUsername == null || desiredUsername.isBlank()) {
      return Result.failure(ApplicationError.validationError("username", "Username must not be null or blank"));
    }
    var username = desiredUsername.trim();
    if (userRepository.existsByUsername(username)) {
      return Result.failure(ApplicationError.conflict(
          USER_RESOURCE,
          "A user with username %s already exists".formatted(username)));
    }

    var user = User.registeredWithGoogle(username, googleUserInfo.subject(), toPersistedRole(role));
    return registerWithProfile(user, profileProvisioner).map(this::authenticate);
  }

  /**
   * Persists a new account and provisions its concrete profile in the current transaction.
   *
   * <p>The account must be saved first, because the profile is keyed by its identifier. When the
   * profiles context then rejects the profile, the transaction is marked rollback-only so that
   * the account insert is discarded as well, and the rejection is returned as a failure instead
   * of an exception, keeping the error a clean 4xx for the caller.</p>
   *
   * @param user               the new account, not yet persisted
   * @param profileProvisioner creates the profile of the persisted account and returns its identifier
   * @return the persisted account, or the reason the registration was rejected
   */
  private Result<User, ApplicationError> registerWithProfile(User user, Function<User, Long> profileProvisioner) {
    var savedUser = userRepository.save(user);
    try {
      var profileId = profileProvisioner.apply(savedUser);
      log.info("Registered user {} as {} through {} with profile {}",
          savedUser.getId(), savedUser.getRoleName(), savedUser.getProvider(), profileId);
      return Result.success(savedUser);
    } catch (IllegalArgumentException exception) {
      markRegistrationForRollback();
      return Result.failure(ApplicationError.validationError(PROFILE_RESOURCE, exception.getMessage()));
    } catch (IllegalStateException exception) {
      markRegistrationForRollback();
      return Result.failure(ApplicationError.conflict(PROFILE_RESOURCE, exception.getMessage()));
    }
  }

  /**
   * Creates the owner profile of a persisted account through the outbound ACL service.
   *
   * @param user           the persisted account, carrying its assigned identifier
   * @param fullName       display name of the account holder
   * @param email          email address of the account holder, stored on the profile
   * @param contactDetails the phone number and address captured by the onboarding form
   * @param ruc            the owner's taxpayer registration number
   * @return the identifier of the created profile
   */
  private Long createOwnerProfile(User user, String fullName, String email, ContactDetails contactDetails, Long ruc) {
    return externalProfileService.createOwnerProfile(
        user.getId(),
        fullName,
        email,
        contactDetails.phone(),
        contactDetails.street(),
        contactDetails.number(),
        contactDetails.city(),
        contactDetails.postalCode(),
        contactDetails.country(),
        ruc);
  }

  /**
   * Creates the technician profile of a persisted account through the outbound ACL service.
   *
   * @param user                the persisted account, carrying its assigned identifier
   * @param fullName            display name of the account holder
   * @param email               email address of the account holder, stored on the profile
   * @param contactDetails      the phone number and address captured by the onboarding form
   * @param speciality          the technician's speciality
   * @param certificationNumber the number of the technician's certification
   * @return the identifier of the created profile
   */
  private Long createTechnicianProfile(
      User user,
      String fullName,
      String email,
      ContactDetails contactDetails,
      String speciality,
      String certificationNumber) {
    return externalProfileService.createTechnicianProfile(
        user.getId(),
        fullName,
        email,
        contactDetails.phone(),
        contactDetails.street(),
        contactDetails.number(),
        contactDetails.city(),
        contactDetails.postalCode(),
        contactDetails.country(),
        speciality,
        certificationNumber);
  }

  /**
   * Discards everything written by the current registration once the method returns.
   *
   * <p>Marking the transaction locally rollback-only makes the transaction manager roll back
   * silently, instead of raising an {@code UnexpectedRollbackException} for a rejection that has
   * already been turned into a {@link Result}.</p>
   */
  private static void markRegistrationForRollback() {
    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
  }

  /**
   * Validates a Google id_token.
   *
   * @param idToken the token submitted by the frontend
   * @return the verified claims, or a validation failure when the token is rejected
   */
  private Result<GoogleUserInfo, ApplicationError> verifyGoogleIdToken(String idToken) {
    try {
      return Result.success(googleTokenService.verify(idToken));
    } catch (IllegalArgumentException exception) {
      return Result.failure(ApplicationError.validationError("idToken", exception.getMessage()));
    }
  }

  /**
   * Resolves the platform account matching a verified Google identity.
   *
   * <p>The Google {@code sub} claim drives the primary lookup because it is stable even when the
   * account owner changes the email. The email is used as a fallback so that an account created
   * locally is linked instead of duplicated; since the profiles context owns email addresses, the
   * account is resolved through the profile that uses the Google email.</p>
   *
   * @param googleUserInfo the verified Google claims
   * @return the matching user, or empty when the Google account is unknown
   */
  private Optional<User> resolveExistingUser(GoogleUserInfo googleUserInfo) {
    var bySubject = userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, googleUserInfo.subject());
    if (bySubject.isPresent()) {
      return bySubject;
    }
    return externalProfileService.fetchUserIdByEmail(googleUserInfo.email()).flatMap(userRepository::findById);
  }

  /**
   * Links an account found by email to the Google identity that is signing in.
   *
   * <p>An account created locally and now signing in with the same Google email keeps its
   * identifier, credentials and role; it simply starts accepting the federated provider.</p>
   *
   * @param user           the matching account
   * @param googleUserInfo the verified Google claims
   * @return the account, persisted again when it had to be linked
   */
  private User linkGoogleAccountIfNeeded(User user, GoogleUserInfo googleUserInfo) {
    if (googleUserInfo.subject().equals(user.getExternalId())) {
      return user;
    }
    user.linkGoogleAccount(googleUserInfo.subject());
    return userRepository.save(user);
  }

  /**
   * Opens a session for an account: issues the platform bearer token, carrying its definitive
   * role as a claim, together with a new refresh token.
   *
   * @param user the authenticated, persisted account
   * @return the account paired with its session tokens
   */
  private ImmutablePair<User, SessionTokens> authenticate(User user) {
    return openSession(user, refreshTokenService.generateToken());
  }

  /**
   * Opens a session for an account with a given raw refresh token: persists the token and issues
   * the matching access token.
   *
   * <p>Only the digest of the refresh token is persisted; the raw value is returned to be handed to
   * the client once.</p>
   *
   * @param user            the authenticated, persisted account
   * @param rawRefreshToken the raw refresh token of the new session
   * @return the account paired with its session tokens
   */
  private ImmutablePair<User, SessionTokens> openSession(User user, String rawRefreshToken) {
    refreshTokenRepository.save(RefreshToken.issue(
        user.getId(),
        refreshTokenService.hashToken(rawRefreshToken),
        refreshTokenService.calculateExpiryDate(Instant.now())));
    var accessToken = tokenService.generateToken(user.getUsername(), user.getRoleName());
    return ImmutablePair.of(user, new SessionTokens(accessToken, rawRefreshToken));
  }

  /**
   * Classifies a refresh token presented after it was revoked.
   *
   * <ul>
   *   <li>Revoked without a replacement (sign-out, or revocation of every session):
   *       {@code REFRESH_TOKEN_REVOKED}.</li>
   *   <li>Rotated within the grace period: a concurrent refresh by the legitimate client,
   *       {@code REFRESH_TOKEN_RECENTLY_ROTATED}.</li>
   *   <li>Rotated before the grace period: either the legitimate client or an attacker holds a
   *       stolen copy, and the two cannot be told apart, so every active session of the account is
   *       revoked, {@code REFRESH_TOKEN_REPLAY_DETECTED}.</li>
   * </ul>
   *
   * @param refreshToken the revoked refresh token
   * @return the exception to throw
   */
  private RefreshTokenException rejectRevokedToken(RefreshToken refreshToken) {
    if (!refreshToken.isRotated()) {
      return new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_REVOKED);
    }
    if (refreshToken.isWithinGracePeriod(refreshTokenService.getReuseGracePeriod())) {
      return new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_RECENTLY_ROTATED);
    }
    log.warn("Rotated refresh token replayed for user {}; revoking all of its sessions", refreshToken.getUserId());
    refreshTokenRepository.revokeAllByUserId(refreshToken.getUserId());
    return new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_REPLAY_DETECTED);
  }

  /**
   * Builds the failure telling the caller that the Google account must complete the onboarding.
   *
   * <p>The code ends with {@code _NOT_FOUND} so it is rendered as a 404, while remaining distinct
   * from a generic missing user for the frontend.</p>
   *
   * @return the onboarding required failure
   */
  private static ApplicationError onboardingRequired() {
    return new ApplicationError(
        "GOOGLE_ACCOUNT_NOT_FOUND",
        "Google account not registered",
        "Complete the onboarding through POST /authentication/google/complete-registration/owner "
            + "or /authentication/google/complete-registration/technician");
  }

  /**
   * Resolves the persisted role row of a role name, so that the user is linked to the seeded
   * role instead of creating a detached duplicate.
   *
   * @param roleName the definitive role
   * @return the persisted role
   */
  private Role toPersistedRole(Roles roleName) {
    return roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));
  }
}
