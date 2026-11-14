package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.authorization;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalServiceRequestService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.OwnerQueryService;
import pe.edu.upc.ice.track.platform.profiles.application.queryservices.TechnicianQueryService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetOwnerByUserIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByIdQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetTechnicianByUserIdQuery;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OTHER_USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USERNAME;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;

@ExtendWith(MockitoExtension.class)
class ProfileAccessEvaluatorTest {

  /** The owner and technician fixtures both belong to account USER_ID; callers use OTHER_USER_ID. */
  private static final String CALLER = "caller";

  @Mock
  private OwnerQueryService ownerQueryService;
  @Mock
  private TechnicianQueryService technicianQueryService;
  @Mock
  private ExternalIamService externalIamService;
  @Mock
  private ExternalServiceRequestService externalServiceRequestService;

  @InjectMocks
  private ProfileAccessEvaluator evaluator;

  private Authentication authentication;

  @BeforeEach
  void setUp() {
    authentication = mock(Authentication.class);
    lenient().when(authentication.isAuthenticated()).thenReturn(true);
    lenient().when(authentication.getName()).thenReturn(CALLER);
  }

  private void callerIsAccount(Long userId) {
    lenient().when(externalIamService.fetchUserIdByUsername(CALLER)).thenReturn(Optional.ofNullable(userId));
  }

  private static Authentication anonymous() {
    var anonymous = mock(Authentication.class);
    when(anonymous.isAuthenticated()).thenReturn(false);
    return anonymous;
  }

  @Test
  @DisplayName("isOwnerSelf(ownerId, auth) -> true only when the caller's account owns that profile (AAA)")
  void ownerSelf() {
    // Arrange
    when(ownerQueryService.handle(new GetOwnerByIdQuery(OWNER_ID))).thenReturn(Optional.of(persistedOwner()));
    callerIsAccount(USER_ID);

    // Act & Assert
    assertThat(evaluator.isOwnerSelf(OWNER_ID, authentication)).isTrue();
  }

  @Test
  @DisplayName("isOwnerSelf(ownerId, auth) of someone else, unknown owner, null id or anonymous -> false (AAA)")
  void ownerSelfDenied() {
    // Arrange
    when(ownerQueryService.handle(new GetOwnerByIdQuery(OWNER_ID))).thenReturn(Optional.of(persistedOwner()));
    lenient().doReturn(Optional.empty()).when(ownerQueryService).handle(new GetOwnerByIdQuery(99L));
    callerIsAccount(OTHER_USER_ID);

    // Act & Assert
    assertThat(evaluator.isOwnerSelf(OWNER_ID, authentication)).isFalse();
    assertThat(evaluator.isOwnerSelf(99L, authentication)).isFalse();
    assertThat(evaluator.isOwnerSelf(null, authentication)).isFalse();
    assertThat(evaluator.isOwnerSelf(OWNER_ID, anonymous())).isFalse();
    assertThat(evaluator.isOwnerSelf(OWNER_ID, null)).isFalse();
  }

  @Test
  @DisplayName("isTechnicianSelf(technicianId, auth) -> true for the technician's own account, false otherwise (AAA)")
  void technicianSelf() {
    // Arrange
    when(technicianQueryService.handle(new GetTechnicianByIdQuery(TECHNICIAN_ID)))
        .thenReturn(Optional.of(persistedTechnician()));
    lenient().doReturn(Optional.empty()).when(technicianQueryService).handle(new GetTechnicianByIdQuery(99L));

    // Act & Assert
    callerIsAccount(USER_ID);
    assertThat(evaluator.isTechnicianSelf(TECHNICIAN_ID, authentication)).isTrue();
    callerIsAccount(OTHER_USER_ID);
    assertThat(evaluator.isTechnicianSelf(TECHNICIAN_ID, authentication)).isFalse();
    assertThat(evaluator.isTechnicianSelf(99L, authentication)).isFalse();
    assertThat(evaluator.isTechnicianSelf(null, authentication)).isFalse();
  }

  @Test
  @DisplayName("isAccountSelf(userId, auth) -> true when the caller is that account (AAA)")
  void accountSelf() {
    // Arrange
    callerIsAccount(USER_ID);

    // Act & Assert
    assertThat(evaluator.isAccountSelf(USER_ID, authentication)).isTrue();
    assertThat(evaluator.isAccountSelf(OTHER_USER_ID, authentication)).isFalse();
    assertThat(evaluator.isAccountSelf(null, authentication)).isFalse();
    assertThat(evaluator.isAccountSelf(USER_ID, anonymous())).isFalse();
  }

  @Test
  @DisplayName("isAccountSelf(userId, auth) caller without a known account -> false (AAA)")
  void accountSelfUnknownCaller() {
    // Arrange
    callerIsAccount(null);

    // Act & Assert
    assertThat(evaluator.isAccountSelf(USER_ID, authentication)).isFalse();
  }

  @Test
  @DisplayName("ownerProfileViewFor(owner, auth) -> FULL for the owner themself (AAA)")
  void ownerViewFull() {
    // Arrange
    callerIsAccount(USER_ID);

    // Act & Assert
    assertThat(evaluator.ownerProfileViewFor(persistedOwner(), authentication)).isEqualTo(ProfileView.FULL);
    verifyNoInteractions(technicianQueryService, externalServiceRequestService);
  }

  @Test
  @DisplayName("ownerProfileViewFor(owner, auth) -> CONTACT for a technician engaged with the owner (AAA)")
  void ownerViewContact() {
    // Arrange
    var technician = persistedTechnician();
    callerIsAccount(OTHER_USER_ID);
    when(technicianQueryService.handle(new GetTechnicianByUserIdQuery(new UserId(OTHER_USER_ID))))
        .thenReturn(Optional.of(technician));
    when(externalServiceRequestService.hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID)).thenReturn(true);

    // Act & Assert
    assertThat(evaluator.ownerProfileViewFor(persistedOwner(), authentication)).isEqualTo(ProfileView.CONTACT);
  }

  @Test
  @DisplayName("ownerProfileViewFor(owner, auth) -> NONE for non-engaged technicians, non-technicians and anonymous (AAA)")
  void ownerViewNone() {
    // Arrange
    callerIsAccount(OTHER_USER_ID);
    when(technicianQueryService.handle(new GetTechnicianByUserIdQuery(new UserId(OTHER_USER_ID))))
        .thenReturn(Optional.of(persistedTechnician()), Optional.empty());
    when(externalServiceRequestService.hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID)).thenReturn(false);

    // Act & Assert
    assertThat(evaluator.ownerProfileViewFor(persistedOwner(), authentication)).isEqualTo(ProfileView.NONE);
    assertThat(evaluator.ownerProfileViewFor(persistedOwner(), authentication)).isEqualTo(ProfileView.NONE);
    assertThat(evaluator.ownerProfileViewFor(persistedOwner(), anonymous())).isEqualTo(ProfileView.NONE);
  }

  @Test
  @DisplayName("technicianProfileViewFor(technician, auth) -> FULL for the technician themself (AAA)")
  void technicianViewFull() {
    // Arrange
    callerIsAccount(USER_ID);

    // Act & Assert
    assertThat(evaluator.technicianProfileViewFor(persistedTechnician(), authentication)).isEqualTo(ProfileView.FULL);
    verifyNoInteractions(ownerQueryService, externalServiceRequestService);
  }

  @Test
  @DisplayName("technicianProfileViewFor(technician, auth) -> CONTACT for an engaged owner, PUBLIC for another owner (AAA)")
  void technicianViewForOwners() {
    // Arrange
    callerIsAccount(OTHER_USER_ID);
    when(ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(OTHER_USER_ID))))
        .thenReturn(Optional.of(persistedOwner()));
    when(externalServiceRequestService.hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID)).thenReturn(true, false);

    // Act & Assert
    assertThat(evaluator.technicianProfileViewFor(persistedTechnician(), authentication)).isEqualTo(ProfileView.CONTACT);
    assertThat(evaluator.technicianProfileViewFor(persistedTechnician(), authentication)).isEqualTo(ProfileView.PUBLIC);
  }

  @Test
  @DisplayName("technicianProfileViewFor(technician, auth) -> NONE for non-owners and anonymous callers (AAA)")
  void technicianViewNone() {
    // Arrange
    callerIsAccount(OTHER_USER_ID);
    when(ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(OTHER_USER_ID)))).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(evaluator.technicianProfileViewFor(persistedTechnician(), authentication)).isEqualTo(ProfileView.NONE);
    assertThat(evaluator.technicianProfileViewFor(persistedTechnician(), anonymous())).isEqualTo(ProfileView.NONE);
  }

  @Test
  @DisplayName("resolveOwnerProfile(auth) -> the caller's owner profile, empty for non-owners and anonymous (AAA)")
  void resolveOwnerProfile() {
    // Arrange
    var owner = persistedOwner();
    callerIsAccount(USER_ID);
    when(ownerQueryService.handle(new GetOwnerByUserIdQuery(new UserId(USER_ID)))).thenReturn(Optional.of(owner));

    // Act & Assert
    assertThat(evaluator.resolveOwnerProfile(authentication)).containsSame(owner);
    assertThat(evaluator.resolveOwnerProfile(anonymous())).isEmpty();
  }

  @Test
  @DisplayName("resolveAccountId(auth) -> the caller's account id, empty when anonymous (AAA)")
  void resolveAccountId() {
    // Arrange
    callerIsAccount(USER_ID);

    // Act & Assert
    assertThat(evaluator.resolveAccountId(authentication)).contains(USER_ID);
    assertThat(evaluator.resolveAccountId(anonymous())).isEmpty();
    assertThat(evaluator.resolveAccountId(null)).isEmpty();
    assertThat(USERNAME).isNotEqualTo(CALLER);
  }
}
