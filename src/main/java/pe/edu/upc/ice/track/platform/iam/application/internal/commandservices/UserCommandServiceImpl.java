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
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.LinkGoogleAccountCommand;
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
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ClientChannel;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.ContactDetails;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.Roles;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.SessionTokens;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RefreshTokenRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * User command service implementation.
 *
 * <p>Owns the authentication and registration flows of the IAM bounded context:</p>
 * <ul>
 *   <li><strong>Local</strong> - role explicit sign-up ({@link SignUpOwnerCommand},
 *       {@link SignUpTechnicianCommand}); sign-in by username or email verified against the
 *       hashing service.</li>
 *   <li><strong>Google, deferred registration</strong> - the frontend performs the Google login
 *       and submits the OIDC id_token. {@link SignInByGoogleCommand} signs a known account in, or
 *       reports that the onboarding is required without writing anything. The role explicit
 *       {@link CompleteGoogleOwnerRegistrationCommand} or
 *       {@link CompleteGoogleTechnicianRegistrationCommand} then creates the account. Tokens are
 *       validated statelessly through the {@link GoogleTokenService} port, backed by Spring
 *       Security's {@code NimbusJwtDecoder}.</li>
 *   <li><strong>Google, explicit linking</strong> - a Google identity is never bound silently to
 *       an account registered with a password. When the verified Google email belongs to such an
 *       account, the Google flows answer {@code ACCOUNT_LINK_REQUIRED}, and only
 *       {@link LinkGoogleAccountCommand}, which carries the account's current password, links
 *       it. An email Google has not verified is never matched against existing accounts.</li>
 * </ul>
 *
 * <p><strong>Credential failures.</strong> An unknown account, an account without a local
 * password and a wrong password are all answered with the same {@code 401 BAD_CREDENTIALS}. When
 * no stored hash exists, the submitted password is compared against a dummy hash computed at
 * startup with the same encoder, so every attempt spends the same hashing work and response time
 * does not reveal whether an account exists.</p>
 *
 * <p><strong>Role-channel authorization.</strong> Every sign-in states the {@link ClientChannel}
 * it comes from, and a session is only opened when the account's role is the one the channel
 * admits; otherwise the answer is {@code 403 ROLE_CHANNEL_MISMATCH} and no token is issued. The
 * channel is checked after the credentials, so it never discloses the role of an account to a
 * caller that does not know its password. The channel is stored with the refresh token and
 * enforced again on every refresh.</p>
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

  /**
   * Hash of a random value nobody knows, computed once with the configured encoder so it costs
   * exactly as much to verify as a real password hash. Compared against whenever an account has
   * no stored hash to compare with.
   */
  private final String dummyPasswordHash;

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
    this.dummyPasswordHash = hashingService.encode(UUID.randomUUID().toString());
  }

  // inherited javadoc
  @Override
  @Transactional // not read-only: a successful sign-in persists the refresh token of the new session
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(SignInByLocalCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Sign-in command must not be null"));
    }
    var identifier = command.username().trim();
    var candidate = findAccountBySignInIdentifier(identifier);
    if (!passwordMatches(candidate, command.password())) {
      log.warn("Rejected local sign-in for identifier fingerprint {}", fingerprint(identifier));
      return Result.failure(AuthenticationErrors.badCredentials());
    }
    return enforceChannel(candidate.get(), command.channel())
        .map(user -> authenticate(user, command.channel()));
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
    return verifyGoogleIdToken(command.idToken())
        .flatMap(this::resolveExistingUser)
        // Deferred registration: nothing is persisted until the onboarding form is completed.
        .flatMap(existingUser -> existingUser
            .map(user -> enforceChannel(user, command.channel()))
            .orElseGet(() -> Result.<User, ApplicationError>failure(AuthenticationErrors.googleAccountNotFound())))
        .map(user -> authenticate(user, command.channel()));
  }

  // inherited javadoc
  @Override
  @Transactional
  public Result<ImmutablePair<User, SessionTokens>, ApplicationError> handle(LinkGoogleAccountCommand command) {
    if (command == null) {
      return Result.failure(ApplicationError.validationError("command", "Google account link command must not be null"));
    }
    return verifyGoogleIdToken(command.idToken()).flatMap(googleUserInfo -> {
      var alreadyLinked = userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, googleUserInfo.subject());
      if (alreadyLinked.isPresent()) {
        // A repeated request: the Google identity already belongs to an account, which signs in.
        return enforceChannel(alreadyLinked.get(), command.channel())
            .map(user -> authenticate(user, command.channel()));
      }
      if (!googleUserInfo.emailVerified()) {
        return Result.failure(AuthenticationErrors.invalidGoogleToken());
      }
      // The account is resolved from the verified Google email only, and must prove its password.
      var candidate = externalProfileService.fetchUserIdByEmail(googleUserInfo.email()).flatMap(userRepository::findById);
      if (!passwordMatches(candidate, command.password())) {
        log.warn("Rejected Google account link for email fingerprint {}", fingerprint(googleUserInfo.email()));
        return Result.failure(AuthenticationErrors.badCredentials());
      }
      return enforceChannel(candidate.get(), command.channel()).map(user -> {
        user.linkGoogleAccount(googleUserInfo.subject());
        var linkedUser = userRepository.save(user);
        log.info("Linked user {} to its Google identity after password confirmation", linkedUser.getId());
        return authenticate(linkedUser, command.channel());
      });
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

    // Checked before rotating as well: a rejected refresh leaves the presented token usable on the
    // channel it belongs to. Tokens issued before the channel was recorded take the role's channel.
    var sessionChannel = refreshToken.getChannel() != null ? refreshToken.getChannel() : ClientChannel.requiredFor(user);
    var statedChannelDiffers = command.channel() != null && command.channel() != sessionChannel;
    if (statedChannelDiffers || !sessionChannel.admits(user)) {
      log.info("Refresh of a {} session of user {} rejected: role-channel mismatch", sessionChannel, user.getId());
      return Result.failure(AuthenticationErrors.roleChannelMismatch(ClientChannel.requiredFor(user)));
    }

    var newRawToken = refreshTokenService.generateToken();
    refreshToken.rotate(refreshTokenService.hashToken(newRawToken));
    // Atomic compare-and-set: when the same token is presented concurrently, only one request
    // rotates it. Losing the race means another request rotated it a few milliseconds ago, which
    // is the concurrent refresh case, never a replay.
    if (!refreshTokenRepository.saveRotation(refreshToken)) {
      throw new RefreshTokenException(AuthErrorCode.REFRESH_TOKEN_RECENTLY_ROTATED);
    }
    return Result.success(openSession(user, newRawToken, sessionChannel));
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
   * Resolves the account a local sign-in identifies.
   *
   * <p>An identifier shaped like an email address is first looked up, lowercased, as the email of
   * a profile; when no profile uses it, or for any other identifier, it is looked up as a
   * username.</p>
   *
   * @param identifier the trimmed username or email address
   * @return the matching account, or empty when none matches
   */
  private Optional<User> findAccountBySignInIdentifier(String identifier) {
    if (identifier.indexOf('@') > 0) {
      var byEmail = externalProfileService.fetchUserIdByEmail(identifier.toLowerCase(Locale.ROOT))
          .flatMap(userRepository::findById);
      if (byEmail.isPresent()) {
        return byEmail;
      }
    }
    return userRepository.findByUsername(identifier);
  }

  /**
   * Verifies a password against an account while spending the same hashing work whether or not
   * the account exists.
   *
   * <p>The comparison always runs: against the stored hash when the account has one, otherwise
   * against {@link #dummyPasswordHash}. Only then is the outcome combined with the existence
   * checks, so the response time of an unknown account matches that of a wrong password.</p>
   *
   * @param candidate   the account the request identifies, or empty when none matches
   * @param rawPassword the submitted password; {@code null} is compared as an empty string
   * @return {@code true} only when the account exists, has a local password, and it matches
   */
  private boolean passwordMatches(Optional<User> candidate, String rawPassword) {
    var encodedPassword = candidate
        .filter(User::hasLocalPassword)
        .map(User::getPassword)
        .orElse(dummyPasswordHash);
    var matches = hashingService.matches(rawPassword == null ? "" : rawPassword, encodedPassword);
    return matches && candidate.isPresent() && candidate.get().hasLocalPassword();
  }

  /**
   * Opens the session only when the account's role is the one the channel admits.
   *
   * @param user    the authenticated account
   * @param channel the client application the request comes from
   * @return the account, or {@code ROLE_CHANNEL_MISMATCH} naming the application it must use
   */
  private static Result<User, ApplicationError> enforceChannel(User user, ClientChannel channel) {
    if (channel.admits(user)) {
      return Result.success(user);
    }
    log.info("Sign-in of user {} holding {} rejected on the {} channel", user.getId(), user.getRoleName(), channel);
    return Result.failure(AuthenticationErrors.roleChannelMismatch(ClientChannel.requiredFor(user)));
  }

  /**
   * Derives a short, non-reversible tag of a sign-in identifier, so failed attempts can be
   * correlated in the logs without writing usernames or email addresses.
   *
   * @param identifier the submitted identifier
   * @return the tag
   */
  private static String fingerprint(String identifier) {
    return Integer.toHexString(identifier.toLowerCase(Locale.ROOT).hashCode());
  }

  /**
   * Registers an account with local credentials and the role implied by the calling command.
   *
   * @param username           the unique username
   * @param password           the raw password, hashed before it reaches the aggregate
   * @param email              the normalized email address; owned by the profiles context, which
   *                            stores it on the profile, so its uniqueness is checked there
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
   * <p>The {@code desiredUsername} comes from the onboarding form. The Google email is never used
   * as username; it is only stored on the profile as contact data.</p>
   *
   * <p>The endpoint's role implies its channel, so an account that already exists only signs in
   * when its role is the endpoint's role; otherwise the answer is {@code ROLE_CHANNEL_MISMATCH}.
   * A verified email that belongs to an account registered with a password is answered with
   * {@code ACCOUNT_LINK_REQUIRED}, never registered again nor linked silently.</p>
   *
   * @param googleUserInfo     the verified Google claims
   * @param desiredUsername    the username requested in the onboarding form
   * @param role               the definitive role implied by the command
   * @param profileProvisioner creates the profile matching {@code role} for the persisted account
   * @return the authenticated user with its session tokens, or the reason the registration failed
   */
  private Result<ImmutablePair<User, SessionTokens>, ApplicationError> completeGoogleRegistration(
      GoogleUserInfo googleUserInfo, String desiredUsername, Roles role, Function<User, Long> profileProvisioner) {
    var channel = ClientChannel.forRole(role);
    return resolveExistingUser(googleUserInfo).flatMap(existingUser -> {
      if (existingUser.isPresent()) {
        // A repeated submission, or an account registered meanwhile: the role is immutable, so the
        // account only signs in through the endpoint matching the role it already has.
        return enforceChannel(existingUser.get(), channel).map(user -> authenticate(user, channel));
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
      return registerWithProfile(user, profileProvisioner).map(savedUser -> authenticate(savedUser, channel));
    });
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
   * <p>Whatever the reason the verifier rejects the token - forged signature, wrong audience,
   * expired, malformed or unverified email - the caller receives the same
   * {@code 401 INVALID_TOKEN}; the reason is only logged.</p>
   *
   * @param idToken the token submitted by the frontend
   * @return the verified claims, or {@code INVALID_TOKEN} when the token is rejected
   */
  private Result<GoogleUserInfo, ApplicationError> verifyGoogleIdToken(String idToken) {
    try {
      return Result.success(googleTokenService.verify(idToken));
    } catch (IllegalArgumentException exception) {
      log.info("Google id_token rejected: {}", exception.getMessage());
      return Result.failure(AuthenticationErrors.invalidGoogleToken());
    }
  }

  /**
   * Resolves the platform account matching a verified Google identity, without ever linking one.
   *
   * <ul>
   *   <li>The Google {@code sub} claim drives the lookup because it is stable even when the
   *       account owner changes the email: an account already bound to it is returned.</li>
   *   <li>An email Google has not verified is never matched against existing accounts, so the
   *       identity counts as unknown.</li>
   *   <li>A verified email used by the profile of an account bound to no such subject is
   *       {@code ACCOUNT_LINK_REQUIRED}: binding it silently would let whoever registered that
   *       email with a password keep access to the account.</li>
   * </ul>
   *
   * @param googleUserInfo the verified Google claims
   * @return the account bound to the Google identity, empty when the identity is unknown, or
   *         {@code ACCOUNT_LINK_REQUIRED}
   */
  private Result<Optional<User>, ApplicationError> resolveExistingUser(GoogleUserInfo googleUserInfo) {
    var bySubject = userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, googleUserInfo.subject());
    if (bySubject.isPresent()) {
      return Result.success(bySubject);
    }
    if (!googleUserInfo.emailVerified()) {
      return Result.success(Optional.empty());
    }
    if (externalProfileService.fetchUserIdByEmail(googleUserInfo.email()).flatMap(userRepository::findById).isPresent()) {
      return Result.failure(AuthenticationErrors.accountLinkRequired());
    }
    return Result.success(Optional.empty());
  }

  /**
   * Opens a session for an account: issues the platform bearer token, carrying its definitive
   * role as a claim, together with a new refresh token bound to the channel.
   *
   * @param user    the authenticated, persisted account, already admitted by {@code channel}
   * @param channel the client application the session is opened from
   * @return the account paired with its session tokens
   */
  private ImmutablePair<User, SessionTokens> authenticate(User user, ClientChannel channel) {
    return openSession(user, refreshTokenService.generateToken(), channel);
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
   * @param channel         the client application the session belongs to
   * @return the account paired with its session tokens
   */
  private ImmutablePair<User, SessionTokens> openSession(User user, String rawRefreshToken, ClientChannel channel) {
    refreshTokenRepository.save(RefreshToken.issue(
        user.getId(),
        refreshTokenService.hashToken(rawRefreshToken),
        refreshTokenService.calculateExpiryDate(Instant.now()),
        channel));
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
