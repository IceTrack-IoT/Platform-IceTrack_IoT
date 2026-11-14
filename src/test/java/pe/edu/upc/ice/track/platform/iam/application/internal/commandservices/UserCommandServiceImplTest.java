package pe.edu.upc.ice.track.platform.iam.application.internal.commandservices;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.acl.ExternalProfileService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.google.GoogleTokenService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.hashing.HashingService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens.RefreshTokenService;
import pe.edu.upc.ice.track.platform.iam.application.internal.outboundservices.tokens.TokenService;
import pe.edu.upc.ice.track.platform.iam.domain.exceptions.RefreshTokenException;
import pe.edu.upc.ice.track.platform.iam.domain.model.aggregates.User;
import pe.edu.upc.ice.track.platform.iam.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.RefreshToken;
import pe.edu.upc.ice.track.platform.iam.domain.model.entities.Role;
import pe.edu.upc.ice.track.platform.iam.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RefreshTokenRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.RoleRepository;
import pe.edu.upc.ice.track.platform.iam.domain.repositories.UserRepository;
import pe.edu.upc.ice.track.platform.iam.test.util.TransactionTestSupport;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.iam.test.fixtures.IamDomainFixtures.*;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.failureError;
import static pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils.successValue;

@ExtendWith(MockitoExtension.class)
class UserCommandServiceImplTest {

  private static final Long OTHER_ID = 99L;
  private static final String DUMMY_HASH = "dummy-password-hash";
  private static final String NEW_RAW_TOKEN = "new-raw-refresh-token";

  @Mock
  private UserRepository userRepository;
  @Mock
  private HashingService hashingService;
  @Mock
  private TokenService tokenService;
  @Mock
  private RoleRepository roleRepository;
  @Mock
  private GoogleTokenService googleTokenService;
  @Mock
  private ExternalProfileService externalProfileService;
  @Mock
  private RefreshTokenRepository refreshTokenRepository;
  @Mock
  private RefreshTokenService refreshTokenService;

  private UserCommandServiceImpl service;
  private TransactionTestSupport transaction;

  @BeforeEach
  void setUp() {
    // The constructor derives a dummy hash used to equalize timing for unknown accounts.
    doReturn(DUMMY_HASH).when(hashingService).encode(any(CharSequence.class));
    service = new UserCommandServiceImpl(userRepository, hashingService, tokenService, roleRepository,
        googleTokenService, externalProfileService, refreshTokenRepository, refreshTokenService);
    transaction = new TransactionTestSupport();
  }

   private void stubSessionOpening(User user) {
    when(refreshTokenService.generateToken()).thenReturn(RAW_REFRESH_TOKEN);
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenService.calculateExpiryDate(any(Instant.class))).thenAnswer(
        invocation -> ((Instant) invocation.getArgument(0)).plus(REFRESH_TTL));
    when(tokenService.generateToken(user.getUsername(), user.getRoleName())).thenReturn(ACCESS_TOKEN);
  }

  private RefreshToken capturedSavedRefreshToken() {
    var captor = ArgumentCaptor.forClass(RefreshToken.class);
    verify(refreshTokenRepository, times(1)).save(captor.capture());
    return captor.getValue();
  }

  private void assertSessionOpened(User expectedUser, ClientChannel expectedChannel,
                                   ImmutablePair<User, SessionTokens> pair) {
    assertThat(pair.getLeft()).isSameAs(expectedUser);
    assertThat(pair.getRight().accessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(pair.getRight().refreshToken()).isEqualTo(RAW_REFRESH_TOKEN);
    var saved = capturedSavedRefreshToken();
    assertThat(saved.getUserId()).isEqualTo(expectedUser.getId());
    assertThat(saved.getToken()).isEqualTo(REFRESH_DIGEST);
    assertThat(saved.getChannel()).isEqualTo(expectedChannel);
    assertThat(saved.getExpiryDate()).isAfter(Instant.now());
  }

  @Test
  @DisplayName("UserCommandServiceImpl -> encodes a random value once to build the dummy hash (AAA)")
  void constructorBuildsDummyHash() {
    // Assert (the constructor ran in setUp)
    verify(hashingService, times(1)).encode(anyString());
    verifyNoMoreInteractions(hashingService);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> null command returns a validation error (AAA)")
  void signInLocalNullCommand() {
    // Act
    var result = service.handle((SignInByLocalCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(userRepository, tokenService, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> valid username/password on the WEB channel opens an owner session (AAA)")
  void signInLocalByUsernameSucceeds() {
    // Arrange
    var owner = localOwner();
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(owner));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    stubSessionOpening(owner);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.WEB));

    // Assert
    assertSessionOpened(owner, ClientChannel.WEB, successValue(result));
    verify(externalProfileService, never()).fetchUserIdByEmail(anyString());
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> trims the identifier before looking the user up (AAA)")
  void signInLocalTrimsIdentifier() {
    // Arrange
    var owner = localOwner();
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(owner));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    stubSessionOpening(owner);
    var command = new SignInByLocalCommand(
        "  " + USERNAME + "  ", PASSWORD, ClientChannel.WEB);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(result.isSuccess()).isTrue();
    verify(userRepository, times(1)).findByUsername(USERNAME);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> an e-mail identifier is resolved through the profile e-mail lookup (AAA)")
  void signInLocalByEmailSucceeds() {
    // Arrange
    var owner = localOwner();
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    stubSessionOpening(owner);
    var command = new SignInByLocalCommand(
        "JPerez@Example.com", PASSWORD, ClientChannel.WEB);

    // Act
    var result = service.handle(command);

    // Assert
    assertSessionOpened(owner, ClientChannel.WEB, successValue(result));
    verify(userRepository, never()).findByUsername(anyString());
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> an e-mail without a matching profile falls back to the username lookup (AAA)")
  void signInLocalEmailFallsBackToUsername() {
    // Arrange
    var owner = localOwner();
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.findByUsername(EMAIL)).thenReturn(Optional.of(owner));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    stubSessionOpening(owner);
    var command = new SignInByLocalCommand(
        EMAIL, PASSWORD, ClientChannel.WEB);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(result.isSuccess()).isTrue();
    verify(userRepository, times(1)).findByUsername(EMAIL);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> unknown user is rejected as bad credentials after checking the dummy hash (AAA)")
  void signInLocalUnknownUser() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("BAD_CREDENTIALS");
    verify(hashingService, times(1)).matches(PASSWORD, DUMMY_HASH);
    verifyNoInteractions(tokenService, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> unknown user is rejected even if the dummy comparison reports a match (AAA)")
  void signInLocalUnknownUserNeverAuthenticates() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.empty());
    when(hashingService.matches(PASSWORD, DUMMY_HASH)).thenReturn(true);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("BAD_CREDENTIALS");
    verifyNoInteractions(tokenService, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> wrong password is rejected as bad credentials (AAA)")
  void signInLocalWrongPassword() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(localOwner()));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.WEB));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("BAD_CREDENTIALS");
    assertThat(error.details()).isEqualTo(AuthenticationErrors.BAD_CREDENTIALS_MESSAGE);
    verifyNoInteractions(tokenService, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> an account without local password is compared against the dummy hash and rejected (AAA)")
  void signInLocalGoogleOnlyAccount() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(googleOwner()));
    when(hashingService.matches(PASSWORD, DUMMY_HASH)).thenReturn(true);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("BAD_CREDENTIALS");
    verify(hashingService, times(1)).matches(PASSWORD, DUMMY_HASH);
    verifyNoInteractions(tokenService, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> a null password is compared as an empty string (AAA)")
  void signInLocalNullPassword() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(localOwner()));
    var command = new SignInByLocalCommand(
        USERNAME, null, ClientChannel.WEB);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("BAD_CREDENTIALS");
    verify(hashingService, times(1)).matches("", ENCODED_PASSWORD);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> a technician signing in on WEB gets a role-channel mismatch pointing to mobile (AAA)")
  void signInLocalTechnicianOnWeb() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(localTechnician()));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.WEB));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(error.details()).contains("mobile");
    verifyNoInteractions(tokenService, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> an owner signing in on MOBILE gets a role-channel mismatch pointing to web (AAA)")
  void signInLocalOwnerOnMobile() {
    // Arrange
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(localOwner()));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.MOBILE));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(error.details()).contains("web");
  }

  @Test
  @DisplayName("handle(SignInByLocalCommand) -> a technician on MOBILE opens a MOBILE session (AAA)")
  void signInLocalTechnicianOnMobile() {
    // Arrange
    var technician = localTechnician();
    when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(technician));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    stubSessionOpening(technician);

    // Act
    var result = service.handle(signInByLocalCommand(ClientChannel.MOBILE));

    // Assert
    assertSessionOpened(technician, ClientChannel.MOBILE, successValue(result));
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> null command returns a validation error (AAA)")
  void signUpOwnerNullCommand() {
    // Act
    var result = service.handle((SignUpOwnerCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(userRepository, roleRepository, externalProfileService);
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> persists an encoded LOCAL owner and provisions its profile (AAA)")
  void signUpOwnerSucceeds() {
    // Arrange
    var command = signUpOwnerCommand();
    var details = contactDetails();
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    doReturn(ENCODED_PASSWORD).when(hashingService).encode(PASSWORD);
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.of(ownerRole()));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0)));
    when(externalProfileService.createOwnerProfile(USER_ID, FULL_NAME, EMAIL, details.phone(), details.street(),
        details.number(), details.city(), details.postalCode(), details.country(), RUC)).thenReturn(PROFILE_ID);

    // Act
    var result = service.handle(command);

    // Assert
    var user = successValue(result);
    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.getUsername()).isEqualTo(USERNAME);
    assertThat(user.getPassword()).isEqualTo(ENCODED_PASSWORD);
    assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    assertThat(user.hasRole(Roles.OWNER_ROLE)).isTrue();
    verify(roleRepository, never()).save(any(Role.class));
    verify(userRepository, times(1)).save(any(User.class));
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> creates the role when it has not been seeded yet (AAA)")
  void signUpOwnerCreatesMissingRole() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    doReturn(ENCODED_PASSWORD).when(hashingService).encode(PASSWORD);
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.empty());
    when(roleRepository.save(any(Role.class))).thenReturn(ownerRole());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0)));
    when(externalProfileService.createOwnerProfile(any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any())).thenReturn(PROFILE_ID);

    // Act
    var result = service.handle(signUpOwnerCommand());

    // Assert
    assertThat(result.isSuccess()).isTrue();
    var captor = ArgumentCaptor.forClass(Role.class);
    verify(roleRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getName()).isEqualTo(Roles.OWNER_ROLE);
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> an existing username yields a USER_CONFLICT and nothing is saved (AAA)")
  void signUpOwnerUsernameTaken() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    // Act
    var result = service.handle(signUpOwnerCommand());

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("USER_CONFLICT");
    assertThat(error.details()).isEqualTo("A user with username %s already exists".formatted(USERNAME));
    verify(userRepository, never()).save(any(User.class));
    verifyNoInteractions(externalProfileService, roleRepository);
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> an existing e-mail yields a USER_CONFLICT and nothing is saved (AAA)")
  void signUpOwnerEmailTaken() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(OTHER_ID));

    // Act
    var result = service.handle(signUpOwnerCommand());

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("USER_CONFLICT");
    assertThat(error.details()).isEqualTo("A user with email %s already exists".formatted(EMAIL));
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> invalid profile data returns a validation error and marks the transaction rollback-only (AAA)")
  void signUpOwnerProfileRejectsData() {
    // Arrange
    stubOwnerSignUpUntilProfile();
    when(externalProfileService.createOwnerProfile(any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any())).thenThrow(new IllegalArgumentException("Invalid phone"));

    // Act
    Result<User, ApplicationError> result = transaction.<Result<User, ApplicationError>>call(() -> service.handle(signUpOwnerCommand()));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).isEqualTo("Invalid phone");
    verify(transaction.status(), times(1)).setRollbackOnly();
  }

  @Test
  @DisplayName("handle(SignUpOwnerCommand) -> a profile that already exists returns a PROFILE_CONFLICT and marks rollback-only (AAA)")
  void signUpOwnerProfileConflict() {
    // Arrange
    stubOwnerSignUpUntilProfile();
    when(externalProfileService.createOwnerProfile(any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any())).thenThrow(new IllegalStateException("Profile already exists"));

    // Act
    Result<User, ApplicationError> result = transaction.<Result<User, ApplicationError>>call(() -> service.handle(signUpOwnerCommand()));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("PROFILE_CONFLICT");
    assertThat(error.details()).isEqualTo("Profile already exists");
    verify(transaction.status(), times(1)).setRollbackOnly();
  }

  private void stubOwnerSignUpUntilProfile() {
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    doReturn(ENCODED_PASSWORD).when(hashingService).encode(PASSWORD);
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.of(ownerRole()));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0)));
  }

  // ====================================================================================
  // handle(SignUpTechnicianCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(SignUpTechnicianCommand) -> null command returns a validation error (AAA)")
  void signUpTechnicianNullCommand() {
    // Act
    var result = service.handle((SignUpTechnicianCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(userRepository, roleRepository, externalProfileService);
  }

  @Test
  @DisplayName("handle(SignUpTechnicianCommand) -> persists an encoded LOCAL technician and provisions its profile (AAA)")
  void signUpTechnicianSucceeds() {
    // Arrange
    var details = contactDetails();
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    doReturn(ENCODED_PASSWORD).when(hashingService).encode(PASSWORD);
    when(roleRepository.findByName(Roles.TECHNICIAN_ROLE)).thenReturn(Optional.of(technicianRole()));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0)));
    when(externalProfileService.createTechnicianProfile(USER_ID, FULL_NAME, EMAIL, details.phone(),
        details.street(), details.number(), details.city(), details.postalCode(), details.country(), SPECIALITY,
        CERTIFICATION_NUMBER)).thenReturn(PROFILE_ID);

    // Act
    var result = service.handle(signUpTechnicianCommand());

    // Assert
    var user = successValue(result);
    assertThat(user.getId()).isEqualTo(USER_ID);
    assertThat(user.hasRole(Roles.TECHNICIAN_ROLE)).isTrue();
    assertThat(user.getProvider()).isEqualTo(AuthProvider.LOCAL);
    verify(externalProfileService, never()).createOwnerProfile(any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any());
  }

  @Test
  @DisplayName("handle(SignUpTechnicianCommand) -> an existing username yields a USER_CONFLICT (AAA)")
  void signUpTechnicianUsernameTaken() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    // Act
    var result = service.handle(signUpTechnicianCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("USER_CONFLICT");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(SignUpTechnicianCommand) -> an existing e-mail yields a USER_CONFLICT (AAA)")
  void signUpTechnicianEmailTaken() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(OTHER_ID));

    // Act
    var result = service.handle(signUpTechnicianCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("USER_CONFLICT");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(SignUpTechnicianCommand) -> invalid profile data returns a validation error and marks rollback-only (AAA)")
  void signUpTechnicianProfileRejectsData() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    doReturn(ENCODED_PASSWORD).when(hashingService).encode(PASSWORD);
    when(roleRepository.findByName(Roles.TECHNICIAN_ROLE)).thenReturn(Optional.of(technicianRole()));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0)));
    when(externalProfileService.createTechnicianProfile(any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any())).thenThrow(new IllegalArgumentException("Invalid certification"));

    // Act
    Result<User, ApplicationError> result = transaction.<Result<User, ApplicationError>>call(() -> service.handle(signUpTechnicianCommand()));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).isEqualTo("Invalid certification");
    verify(transaction.status(), times(1)).setRollbackOnly();
  }

  @Test
  @DisplayName("handle(SignUpTechnicianCommand) -> a duplicate profile returns a PROFILE_CONFLICT and marks rollback-only (AAA)")
  void signUpTechnicianProfileConflict() {
    // Arrange
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    doReturn(ENCODED_PASSWORD).when(hashingService).encode(PASSWORD);
    when(roleRepository.findByName(Roles.TECHNICIAN_ROLE)).thenReturn(Optional.of(technicianRole()));
    when(userRepository.save(any(User.class))).thenAnswer(inv -> persisted(inv.getArgument(0)));
    when(externalProfileService.createTechnicianProfile(any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any())).thenThrow(new IllegalStateException("Technician already has a profile"));

    // Act
    Result<User, ApplicationError> result = transaction.<Result<User, ApplicationError>>call(() -> service.handle(signUpTechnicianCommand()));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("PROFILE_CONFLICT");
    verify(transaction.status(), times(1)).setRollbackOnly();
  }

  // ====================================================================================
  // handle(SignInByGoogleCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> null command returns a validation error (AAA)")
  void signInGoogleNullCommand() {
    // Act
    var result = service.handle((SignInByGoogleCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(googleTokenService, userRepository);
  }

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> an account already linked to the Google subject opens a session (AAA)")
  void signInGoogleSucceeds() {
    // Arrange
    var owner = googleOwner();
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.of(owner));
    stubSessionOpening(owner);

    // Act
    var result = service.handle(signInByGoogleCommand(ClientChannel.WEB));

    // Assert
    assertSessionOpened(owner, ClientChannel.WEB, successValue(result));
    verifyNoInteractions(externalProfileService);
  }

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> an invalid Google token yields INVALID_TOKEN (AAA)")
  void signInGoogleInvalidToken() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenThrow(new IllegalArgumentException("bad signature"));

    // Act
    var result = service.handle(signInByGoogleCommand(ClientChannel.WEB));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("INVALID_TOKEN");
    assertThat(error.details()).isEqualTo(AuthenticationErrors.INVALID_GOOGLE_TOKEN_MESSAGE);
    verifyNoInteractions(userRepository, refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> an unknown Google identity yields GOOGLE_ACCOUNT_NOT_FOUND (AAA)")
  void signInGoogleUnknownAccount() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(signInByGoogleCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("GOOGLE_ACCOUNT_NOT_FOUND");
    verifyNoInteractions(refreshTokenRepository, tokenService);
  }

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> a verified e-mail owned by a password account yields ACCOUNT_LINK_REQUIRED (AAA)")
  void signInGoogleRequiresLink() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(localOwner()));

    // Act
    var result = service.handle(signInByGoogleCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ACCOUNT_LINK_REQUIRED");
    verifyNoInteractions(refreshTokenRepository, tokenService);
  }

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> an unverified Google e-mail is never matched against profiles (AAA)")
  void signInGoogleUnverifiedEmail() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(unverifiedGoogleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(signInByGoogleCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("GOOGLE_ACCOUNT_NOT_FOUND");
    verifyNoInteractions(externalProfileService);
  }

  @Test
  @DisplayName("handle(SignInByGoogleCommand) -> a technician account on the WEB channel yields ROLE_CHANNEL_MISMATCH (AAA)")
  void signInGoogleRoleChannelMismatch() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB))
        .thenReturn(Optional.of(googleTechnician()));

    // Act
    var result = service.handle(signInByGoogleCommand(ClientChannel.WEB));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(error.details()).contains("mobile");
    verifyNoInteractions(refreshTokenRepository);
  }

  // ====================================================================================
  // handle(LinkGoogleAccountCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> null command returns a validation error (AAA)")
  void linkGoogleNullCommand() {
    // Act
    var result = service.handle((LinkGoogleAccountCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(googleTokenService);
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> an invalid Google token yields INVALID_TOKEN (AAA)")
  void linkGoogleInvalidToken() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenThrow(new IllegalArgumentException("expired"));

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("INVALID_TOKEN");
    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> an identity already linked simply signs the user in without a password check (AAA)")
  void linkGoogleAlreadyLinked() {
    // Arrange
    var owner = googleOwner();
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.of(owner));
    stubSessionOpening(owner);

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertSessionOpened(owner, ClientChannel.WEB, successValue(result));
    verify(userRepository, never()).save(any(User.class));
    verify(hashingService, never()).matches(any(CharSequence.class), anyString());
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> an already linked identity on the wrong channel yields ROLE_CHANNEL_MISMATCH (AAA)")
  void linkGoogleAlreadyLinkedWrongChannel() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB))
        .thenReturn(Optional.of(googleTechnician()));

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> an unverified Google e-mail cannot be linked (AAA)")
  void linkGoogleUnverifiedEmail() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(unverifiedGoogleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("INVALID_TOKEN");
    verifyNoInteractions(externalProfileService);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> confirming the password links the identity and opens a session (AAA)")
  void linkGoogleSucceeds() {
    // Arrange
    var owner = localOwner();
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);
    when(userRepository.save(owner)).thenReturn(owner);
    stubSessionOpening(owner);

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertSessionOpened(owner, ClientChannel.WEB, successValue(result));
    assertThat(owner.getProvider()).isEqualTo(AuthProvider.GOOGLE);
    assertThat(owner.getExternalId()).isEqualTo(GOOGLE_SUB);
    verify(userRepository, times(1)).save(owner);
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> a wrong password yields BAD_CREDENTIALS and does not link (AAA)")
  void linkGoogleWrongPassword() {
    // Arrange
    var owner = localOwner();
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(false);

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("BAD_CREDENTIALS");
    assertThat(owner.getProvider()).isEqualTo(AuthProvider.LOCAL);
    verify(userRepository, never()).save(any(User.class));
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> no account for the e-mail yields BAD_CREDENTIALS after a dummy comparison (AAA)")
  void linkGoogleNoAccountForEmail() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("BAD_CREDENTIALS");
    verify(hashingService, times(1)).matches(PASSWORD, DUMMY_HASH);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(LinkGoogleAccountCommand) -> a correct password on the wrong channel yields ROLE_CHANNEL_MISMATCH and does not link (AAA)")
  void linkGoogleWrongChannel() {
    // Arrange
    var technician = localTechnician();
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(technician));
    when(hashingService.matches(PASSWORD, ENCODED_PASSWORD)).thenReturn(true);

    // Act
    var result = service.handle(linkGoogleAccountCommand(ClientChannel.WEB));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(technician.getProvider()).isEqualTo(AuthProvider.LOCAL);
    verify(userRepository, never()).save(any(User.class));
  }

  // ====================================================================================
  // handle(CompleteGoogleOwnerRegistrationCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> null command returns a validation error (AAA)")
  void completeOwnerNullCommand() {
    // Act
    var result = service.handle(
        (CompleteGoogleOwnerRegistrationCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(googleTokenService);
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> registers a Google owner, provisions the profile and opens a session (AAA)")
  void completeOwnerSucceeds() {
    // Arrange
    var details = contactDetails();
    var registered = persisted(User.registeredWithGoogle(USERNAME, GOOGLE_SUB, ownerRole()));
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.of(ownerRole()));
    when(userRepository.save(any(User.class))).thenReturn(registered);
    when(externalProfileService.createOwnerProfile(USER_ID, FULL_NAME, EMAIL, details.phone(), details.street(),
        details.number(), details.city(), details.postalCode(), details.country(), RUC)).thenReturn(PROFILE_ID);
    stubSessionOpening(registered);

    // Act
    var result = service.handle(completeGoogleOwnerRegistrationCommand());

    // Assert
    assertSessionOpened(registered, ClientChannel.WEB, successValue(result));
    var captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue().getProvider()).isEqualTo(AuthProvider.GOOGLE);
    assertThat(captor.getValue().getExternalId()).isEqualTo(GOOGLE_SUB);
    assertThat(captor.getValue().hasLocalPassword()).isFalse();
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> trims the requested username before saving (AAA)")
  void completeOwnerTrimsUsername() {
    // Arrange
    var registered = persisted(User.registeredWithGoogle(USERNAME, GOOGLE_SUB, ownerRole()));
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.of(ownerRole()));
    when(userRepository.save(any(User.class))).thenReturn(registered);
    when(externalProfileService.createOwnerProfile(any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any())).thenReturn(PROFILE_ID);
    stubSessionOpening(registered);
    var command = new CompleteGoogleOwnerRegistrationCommand(
        GOOGLE_ID_TOKEN, "  " + USERNAME + "  ", contactDetails(), RUC);

    // Act
    var result = service.handle(command);

    // Assert
    assertThat(result.isSuccess()).isTrue();
    var captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().getUsername()).isEqualTo(USERNAME);
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> an identity already registered just signs in (AAA)")
  void completeOwnerAlreadyRegistered() {
    // Arrange
    var owner = googleOwner();
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.of(owner));
    stubSessionOpening(owner);

    // Act
    var result = service.handle(completeGoogleOwnerRegistrationCommand());

    // Assert
    assertSessionOpened(owner, ClientChannel.WEB, successValue(result));
    verify(userRepository, never()).save(any(User.class));
    verifyNoInteractions(roleRepository);
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> an already registered technician identity yields ROLE_CHANNEL_MISMATCH (AAA)")
  void completeOwnerAlreadyRegisteredAsTechnician() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB))
        .thenReturn(Optional.of(googleTechnician()));

    // Act
    var result = service.handle(completeGoogleOwnerRegistrationCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> an e-mail used by a password account yields ACCOUNT_LINK_REQUIRED (AAA)")
  void completeOwnerRequiresLink() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.of(USER_ID));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(localOwner()));

    // Act
    var result = service.handle(completeGoogleOwnerRegistrationCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("ACCOUNT_LINK_REQUIRED");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> a taken username yields a USER_CONFLICT (AAA)")
  void completeOwnerUsernameTaken() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    // Act
    var result = service.handle(completeGoogleOwnerRegistrationCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("USER_CONFLICT");
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> an invalid Google token yields INVALID_TOKEN (AAA)")
  void completeOwnerInvalidToken() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenThrow(new IllegalArgumentException("bad"));

    // Act
    var result = service.handle(completeGoogleOwnerRegistrationCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("INVALID_TOKEN");
    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("handle(CompleteGoogleOwnerRegistrationCommand) -> invalid profile data returns a validation error and marks rollback-only (AAA)")
  void completeOwnerProfileRejectsData() {
    // Arrange
    var registered = persisted(User.registeredWithGoogle(USERNAME, GOOGLE_SUB, ownerRole()));
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(roleRepository.findByName(Roles.OWNER_ROLE)).thenReturn(Optional.of(ownerRole()));
    when(userRepository.save(any(User.class))).thenReturn(registered);
    when(externalProfileService.createOwnerProfile(any(), any(), any(), any(), any(), any(), any(), any(), any(),
        any())).thenThrow(new IllegalArgumentException("Invalid RUC"));

    // Act
    Result<ImmutablePair<User, SessionTokens>, ApplicationError> result = transaction.<Result<ImmutablePair<User, SessionTokens>, ApplicationError>>call(() -> service.handle(completeGoogleOwnerRegistrationCommand()));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).isEqualTo("Invalid RUC");
    verify(transaction.status(), times(1)).setRollbackOnly();
    verifyNoInteractions(refreshTokenRepository);
  }

  // ====================================================================================
  // handle(CompleteGoogleTechnicianRegistrationCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(CompleteGoogleTechnicianRegistrationCommand) -> null command returns a validation error (AAA)")
  void completeTechnicianNullCommand() {
    // Act
    var result = service.handle(
        (CompleteGoogleTechnicianRegistrationCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
  }

  @Test
  @DisplayName("handle(CompleteGoogleTechnicianRegistrationCommand) -> registers a Google technician and opens a MOBILE session (AAA)")
  void completeTechnicianSucceeds() {
    // Arrange
    var details = contactDetails();
    var registered = persisted(User.registeredWithGoogle(USERNAME, GOOGLE_SUB, technicianRole()));
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(roleRepository.findByName(Roles.TECHNICIAN_ROLE)).thenReturn(Optional.of(technicianRole()));
    when(userRepository.save(any(User.class))).thenReturn(registered);
    when(externalProfileService.createTechnicianProfile(USER_ID, FULL_NAME, EMAIL, details.phone(),
        details.street(), details.number(), details.city(), details.postalCode(), details.country(), SPECIALITY,
        CERTIFICATION_NUMBER)).thenReturn(PROFILE_ID);
    stubSessionOpening(registered);

    // Act
    var result = service.handle(completeGoogleTechnicianRegistrationCommand());

    // Assert
    assertSessionOpened(registered, ClientChannel.MOBILE, successValue(result));
  }

  @Test
  @DisplayName("handle(CompleteGoogleTechnicianRegistrationCommand) -> an already registered owner identity yields ROLE_CHANNEL_MISMATCH (AAA)")
  void completeTechnicianAlreadyRegisteredAsOwner() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB))
        .thenReturn(Optional.of(googleOwner()));

    // Act
    var result = service.handle(completeGoogleTechnicianRegistrationCommand());

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(error.details()).contains("web");
  }

  @Test
  @DisplayName("handle(CompleteGoogleTechnicianRegistrationCommand) -> a taken username yields a USER_CONFLICT (AAA)")
  void completeTechnicianUsernameTaken() {
    // Arrange
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(true);

    // Act
    var result = service.handle(completeGoogleTechnicianRegistrationCommand());

    // Assert
    assertThat(failureError(result).code()).isEqualTo("USER_CONFLICT");
  }

  @Test
  @DisplayName("handle(CompleteGoogleTechnicianRegistrationCommand) -> a duplicate profile returns PROFILE_CONFLICT and marks rollback-only (AAA)")
  void completeTechnicianProfileConflict() {
    // Arrange
    var registered = persisted(User.registeredWithGoogle(USERNAME, GOOGLE_SUB, technicianRole()));
    when(googleTokenService.verify(GOOGLE_ID_TOKEN)).thenReturn(googleUserInfo());
    when(userRepository.findByProviderAndExternalId(AuthProvider.GOOGLE, GOOGLE_SUB)).thenReturn(Optional.empty());
    when(externalProfileService.fetchUserIdByEmail(EMAIL)).thenReturn(Optional.empty());
    when(userRepository.existsByUsername(USERNAME)).thenReturn(false);
    when(roleRepository.findByName(Roles.TECHNICIAN_ROLE)).thenReturn(Optional.of(technicianRole()));
    when(userRepository.save(any(User.class))).thenReturn(registered);
    when(externalProfileService.createTechnicianProfile(any(), any(), any(), any(), any(), any(), any(), any(),
        any(), any(), any())).thenThrow(new IllegalStateException("duplicate"));

    // Act
    Result<ImmutablePair<User, SessionTokens>, ApplicationError> result = transaction.<Result<ImmutablePair<User, SessionTokens>, ApplicationError>>call(() -> service.handle(completeGoogleTechnicianRegistrationCommand()));

    // Assert
    assertThat(failureError(result).code()).isEqualTo("PROFILE_CONFLICT");
    verify(transaction.status(), times(1)).setRollbackOnly();
  }

  // ====================================================================================
  // handle(RefreshTokenCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> null command returns a validation error (AAA)")
  void refreshNullCommand() {
    // Act
    var result = service.handle((RefreshTokenCommand) null);

    // Assert
    assertThat(failureError(result).code()).isEqualTo("VALIDATION_ERROR");
    verifyNoInteractions(refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> rotates an active token and returns a new session on the same channel (AAA)")
  void refreshRotatesToken() {
    // Arrange
    var owner = localOwner();
    var current = activeRefreshToken();
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(current));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(owner));
    when(refreshTokenService.generateToken()).thenReturn(NEW_RAW_TOKEN);
    doReturn(OTHER_REFRESH_DIGEST).when(refreshTokenService).hashToken(NEW_RAW_TOKEN);
    when(refreshTokenRepository.saveRotation(current)).thenReturn(true);
    when(refreshTokenService.calculateExpiryDate(any(Instant.class))).thenReturn(Instant.now().plus(REFRESH_TTL));
    when(tokenService.generateToken(USERNAME, "OWNER_ROLE")).thenReturn(ACCESS_TOKEN);

    // Act
    var result = service.handle(refreshTokenCommand(ClientChannel.WEB));

    // Assert
    var pair = successValue(result);
    assertThat(pair.getLeft()).isSameAs(owner);
    assertThat(pair.getRight().accessToken()).isEqualTo(ACCESS_TOKEN);
    assertThat(pair.getRight().refreshToken()).isEqualTo(NEW_RAW_TOKEN);
    assertThat(current.isRevoked()).isTrue();
    assertThat(current.getReplacedByToken()).isEqualTo(OTHER_REFRESH_DIGEST);
    var saved = capturedSavedRefreshToken();
    assertThat(saved.getToken()).isEqualTo(OTHER_REFRESH_DIGEST);
    assertThat(saved.getChannel()).isEqualTo(ClientChannel.WEB);
    assertThat(saved.getUserId()).isEqualTo(USER_ID);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> without a stated channel the session channel is kept (AAA)")
  void refreshWithoutStatedChannel() {
    // Arrange
    var current = activeRefreshToken();
    stubRotation(current, localOwner());

    // Act
    var result = service.handle(refreshTokenCommand(null));

    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedSavedRefreshToken().getChannel()).isEqualTo(ClientChannel.WEB);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a legacy token without channel is attributed to the channel of the user's role (AAA)")
  void refreshLegacyTokenWithoutChannel() {
    // Arrange
    var technician = localTechnician();
    var legacy = activeRefreshToken(USER_ID, REFRESH_DIGEST, null);
    stubRotation(legacy, technician);

    // Act
    var result = service.handle(refreshTokenCommand(null));

    // Assert
    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedSavedRefreshToken().getChannel()).isEqualTo(ClientChannel.MOBILE);
  }

  private void stubRotation(RefreshToken current, User user) {
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(current));
    when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
    when(refreshTokenService.generateToken()).thenReturn(NEW_RAW_TOKEN);
    doReturn(OTHER_REFRESH_DIGEST).when(refreshTokenService).hashToken(NEW_RAW_TOKEN);
    when(refreshTokenRepository.saveRotation(current)).thenReturn(true);
    when(refreshTokenService.calculateExpiryDate(any(Instant.class))).thenReturn(Instant.now().plus(REFRESH_TTL));
    when(tokenService.generateToken(user.getUsername(), user.getRoleName())).thenReturn(ACCESS_TOKEN);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> an unknown token throws REFRESH_TOKEN_INVALID (AAA)")
  void refreshUnknownToken() {
    // Arrange
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.empty());

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
    verifyNoInteractions(userRepository, tokenService);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> an expired token throws REFRESH_TOKEN_EXPIRED (AAA)")
  void refreshExpiredToken() {
    // Arrange
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(expiredRefreshToken()));

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_EXPIRED);
    verifyNoInteractions(userRepository);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a token revoked by sign-out throws REFRESH_TOKEN_REVOKED (AAA)")
  void refreshRevokedToken() {
    // Arrange
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(revokedRefreshToken()));

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_REVOKED);
    verify(refreshTokenRepository, never()).revokeAllByUserId(any());
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a token rotated within the grace period throws REFRESH_TOKEN_RECENTLY_ROTATED (AAA)")
  void refreshRotatedWithinGrace() {
    // Arrange
    var rotated = rotatedRefreshToken(Instant.now().minusSeconds(1), ClientChannel.WEB);
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(rotated));
    when(refreshTokenService.getReuseGracePeriod()).thenReturn(GRACE_PERIOD);

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_RECENTLY_ROTATED);
    verify(refreshTokenRepository, never()).revokeAllByUserId(any());
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a rotated token replayed after the grace period revokes every session of the user (AAA)")
  void refreshReplayDetected() {
    // Arrange
    var rotated = rotatedRefreshToken(Instant.now().minus(Duration.ofMinutes(10)), ClientChannel.WEB);
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(rotated));
    when(refreshTokenService.getReuseGracePeriod()).thenReturn(GRACE_PERIOD);

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_REPLAY_DETECTED);
    verify(refreshTokenRepository, times(1)).revokeAllByUserId(USER_ID);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a token whose user no longer exists throws REFRESH_TOKEN_INVALID (AAA)")
  void refreshUserMissing() {
    // Arrange
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(activeRefreshToken()));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_INVALID);
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a stated channel that differs from the session channel is rejected without rotating (AAA)")
  void refreshStatedChannelDiffers() {
    // Arrange
    var current = activeRefreshToken();
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(current));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(localOwner()));

    // Act
    var result = service.handle(refreshTokenCommand(ClientChannel.MOBILE));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(error.details()).contains("web");
    assertThat(current.isRevoked()).isFalse();
    verify(refreshTokenRepository, never()).saveRotation(any(RefreshToken.class));
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> a session whose channel no longer admits the user's role is rejected (AAA)")
  void refreshSessionChannelNoLongerAdmitsUser() {
    // Arrange
    var current = activeRefreshToken(USER_ID, REFRESH_DIGEST, ClientChannel.WEB);
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(current));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(localTechnician()));

    // Act
    var result = service.handle(refreshTokenCommand(null));

    // Assert
    var error = failureError(result);
    assertThat(error.code()).isEqualTo("ROLE_CHANNEL_MISMATCH");
    assertThat(error.details()).contains("mobile");
    verify(refreshTokenRepository, never()).saveRotation(any(RefreshToken.class));
  }

  @Test
  @DisplayName("handle(RefreshTokenCommand) -> losing the rotation race throws REFRESH_TOKEN_RECENTLY_ROTATED and issues nothing (AAA)")
  void refreshLosesRotationRace() {
    // Arrange
    var current = activeRefreshToken();
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);
    when(refreshTokenRepository.findByToken(REFRESH_DIGEST)).thenReturn(Optional.of(current));
    when(userRepository.findById(USER_ID)).thenReturn(Optional.of(localOwner()));
    when(refreshTokenService.generateToken()).thenReturn(OTHER_RAW_REFRESH_TOKEN);
    doReturn(OTHER_REFRESH_DIGEST).when(refreshTokenService).hashToken(OTHER_RAW_REFRESH_TOKEN);
    when(refreshTokenRepository.saveRotation(current)).thenReturn(false);

    // Act & Assert
    var ex = assertThrows(RefreshTokenException.class, () -> service.handle(refreshTokenCommand(ClientChannel.WEB)));
    assertThat(ex.getErrorCode()).isEqualTo(AuthErrorCode.REFRESH_TOKEN_RECENTLY_ROTATED);
    verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    verifyNoInteractions(tokenService);
  }

  @Test
  @DisplayName("handle(SignOutCommand) -> revokes the refresh token identified by its digest (AAA)")
  void signOutRevokesToken() {
    // Arrange
    when(refreshTokenService.hashToken(RAW_REFRESH_TOKEN)).thenReturn(REFRESH_DIGEST);

    // Act
    service.handle(signOutCommand());

    // Assert
    verify(refreshTokenRepository, times(1)).revoke(REFRESH_DIGEST);
    verifyNoMoreInteractions(refreshTokenRepository);
  }

  @Test
  @DisplayName("handle(SignOutCommand) -> a null command is ignored (AAA)")
  void signOutNullCommand() {
    // Act
    service.handle((SignOutCommand) null);

    // Assert
    verifyNoInteractions(refreshTokenRepository, refreshTokenService);
  }

  @Test
  @DisplayName("handle(PurgeExpiredRefreshTokensCommand) -> deletes tokens expired before now and returns the count (AAA)")
  void purgeDeletesExpiredTokens() {
    // Arrange
    when(refreshTokenRepository.deleteAllExpiredBefore(any(Instant.class))).thenReturn(3);
    var before = Instant.now();

    // Act
    var purged = service.handle(new PurgeExpiredRefreshTokensCommand());

    // Assert
    assertThat(purged).isEqualTo(3);
    var captor = ArgumentCaptor.forClass(Instant.class);
    verify(refreshTokenRepository, times(1)).deleteAllExpiredBefore(captor.capture());
    assertThat(captor.getValue()).isBetween(before, Instant.now());
  }
}
