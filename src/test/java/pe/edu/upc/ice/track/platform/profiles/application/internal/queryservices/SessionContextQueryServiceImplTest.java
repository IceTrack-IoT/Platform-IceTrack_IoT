package pe.edu.upc.ice.track.platform.profiles.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalAssetsService;
import pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl.ExternalIamService;
import pe.edu.upc.ice.track.platform.profiles.domain.model.queries.GetSessionContextQuery;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.DashboardConfigRepository;
import pe.edu.upc.ice.track.platform.profiles.domain.repositories.ProfileRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.EMAIL;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USERNAME;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedDashboard;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedOwner;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.persistedTechnician;

@ExtendWith(MockitoExtension.class)
class SessionContextQueryServiceImplTest {

  private static final UserId USER = new UserId(USER_ID);
  private static final ExternalIamService.Account ACCOUNT =
      new ExternalIamService.Account(USER_ID, USERNAME, "ROLE_OWNER", "LOCAL");

  @Mock
  private ExternalIamService externalIamService;
  @Mock
  private ExternalAssetsService externalAssetsService;
  @Mock
  private ProfileRepository profileRepository;
  @Mock
  private DashboardConfigRepository dashboardConfigRepository;

  @InjectMocks
  private SessionContextQueryServiceImpl service;

  @Test
  @DisplayName("handle(GetSessionContextQuery) unknown account -> empty and no further lookups (AAA)")
  void unknownAccount() {
    // Arrange
    when(externalIamService.fetchAccountByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(new GetSessionContextQuery(USERNAME));

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(profileRepository, dashboardConfigRepository, externalAssetsService);
  }

  @Test
  @DisplayName("handle(GetSessionContextQuery) owner -> account, owner profile, dashboard and asset counts (AAA)")
  void ownerContext() {
    // Arrange
    var owner = persistedOwner();
    var dashboard = persistedDashboard();
    var before = Instant.now();
    when(externalIamService.fetchAccountByUsername(USERNAME)).thenReturn(Optional.of(ACCOUNT));
    when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(owner));
    when(dashboardConfigRepository.findByUserId(USER)).thenReturn(Optional.of(dashboard));
    when(externalAssetsService.countSites(OWNER_ID)).thenReturn(3);
    when(externalAssetsService.countEquipment(OWNER_ID)).thenReturn(12L);

    // Act
    var context = service.handle(new GetSessionContextQuery(USERNAME)).orElseThrow();

    // Assert
    assertThat(context.userId()).isEqualTo(USER_ID);
    assertThat(context.username()).isEqualTo(USERNAME);
    assertThat(context.role()).isEqualTo("ROLE_OWNER");
    assertThat(context.provider()).isEqualTo("LOCAL");
    assertThat(context.email()).isEqualTo(EMAIL);
    assertThat(context.ownerProfile()).isSameAs(owner);
    assertThat(context.dashboardConfig()).isSameAs(dashboard);
    assertThat(context.siteCount()).isEqualTo(3L);
    assertThat(context.equipmentCount()).isEqualTo(12L);
    assertThat(context.serverTime()).isBetween(before, Instant.now().plusSeconds(1));
  }

  @Test
  @DisplayName("handle(GetSessionContextQuery) technician -> e-mail but no owner profile and zero counts without asset calls (AAA)")
  void technicianContext() {
    // Arrange
    when(externalIamService.fetchAccountByUsername(USERNAME)).thenReturn(Optional.of(ACCOUNT));
    when(profileRepository.findByUserId(USER)).thenReturn(Optional.of(persistedTechnician()));
    when(dashboardConfigRepository.findByUserId(USER)).thenReturn(Optional.empty());

    // Act
    var context = service.handle(new GetSessionContextQuery(USERNAME)).orElseThrow();

    // Assert
    assertThat(context.email()).isEqualTo(EMAIL);
    assertThat(context.ownerProfile()).isNull();
    assertThat(context.dashboardConfig()).isNull();
    assertThat(context.siteCount()).isZero();
    assertThat(context.equipmentCount()).isZero();
    verifyNoInteractions(externalAssetsService);
  }

  @Test
  @DisplayName("handle(GetSessionContextQuery) account without profile -> null e-mail and profile, zero counts (AAA)")
  void accountWithoutProfile() {
    // Arrange
    when(externalIamService.fetchAccountByUsername(USERNAME)).thenReturn(Optional.of(ACCOUNT));
    when(profileRepository.findByUserId(USER)).thenReturn(Optional.empty());
    when(dashboardConfigRepository.findByUserId(USER)).thenReturn(Optional.empty());

    // Act
    var context = service.handle(new GetSessionContextQuery(USERNAME)).orElseThrow();

    // Assert
    assertThat(context.email()).isNull();
    assertThat(context.ownerProfile()).isNull();
    assertThat(context.siteCount()).isZero();
    verifyNoInteractions(externalAssetsService);
  }
}
