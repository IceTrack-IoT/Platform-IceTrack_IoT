package pe.edu.upc.ice.track.platform.profiles.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.AccountSummary;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.OWNER_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.TECHNICIAN_ID;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USERNAME;
import static pe.edu.upc.ice.track.platform.profiles.test.fixtures.ProfilesDomainFixtures.USER_ID;

class ProfilesOutboundServicesTest {

  @Nested
  @ExtendWith(MockitoExtension.class)
  class ExternalAssetsServiceTest {

    @Mock
    private AssetContextFacade assetContextFacade;
    @InjectMocks
    private ExternalAssetsService service;

    @Test
    @DisplayName("countSites(ownerId) / countEquipment(ownerId) -> delegate to the assets facade (AAA)")
    void delegates() {
      // Arrange
      when(assetContextFacade.countSitesByOwner(OWNER_ID)).thenReturn(3);
      when(assetContextFacade.countEquipmentByOwner(OWNER_ID)).thenReturn(12L);

      // Act & Assert
      assertThat(service.countSites(OWNER_ID)).isEqualTo(3);
      assertThat(service.countEquipment(OWNER_ID)).isEqualTo(12L);
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class ExternalServiceRequestServiceTest {

    @Mock
    private ServiceRequestContextFacade serviceRequestContextFacade;
    @InjectMocks
    private ExternalServiceRequestService service;

    @Test
    @DisplayName("hasActiveEngagementBetween(owner, technician) -> answer of the service-request facade (AAA)")
    void delegates() {
      // Arrange
      when(serviceRequestContextFacade.hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID)).thenReturn(true);

      // Act & Assert
      assertThat(service.hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID)).isTrue();
      verify(serviceRequestContextFacade, times(1)).hasActiveEngagementBetween(OWNER_ID, TECHNICIAN_ID);
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class ExternalIamServiceTest {

    @Mock
    private IamContextFacade iamContextFacade;
    @InjectMocks
    private ExternalIamService service;

    @Test
    @DisplayName("fetchUserIdByUsername(null or blank) -> empty without lookup (AAA)")
    void blankUsername() {
      // Act & Assert
      assertThat(service.fetchUserIdByUsername(null)).isEmpty();
      assertThat(service.fetchUserIdByUsername("  ")).isEmpty();
      verifyNoInteractions(iamContextFacade);
    }

    @Test
    @DisplayName("fetchUserIdByUsername(username) -> the id; 0 or null from IAM means no such user (AAA)")
    void userIdLookup() {
      // Arrange
      when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
      doReturn(0L).when(iamContextFacade).fetchUserIdByUsername("ghost");
      doReturn(null).when(iamContextFacade).fetchUserIdByUsername("nobody");

      // Act & Assert
      assertThat(service.fetchUserIdByUsername(USERNAME)).contains(USER_ID);
      assertThat(service.fetchUserIdByUsername("ghost")).isEmpty();
      assertThat(service.fetchUserIdByUsername("nobody")).isEmpty();
    }

    @Test
    @DisplayName("existsUserById(null) -> false without lookup; otherwise the IAM answer (AAA)")
    void existsUser() {
      // Arrange
      when(iamContextFacade.existsUserById(USER_ID)).thenReturn(true);

      // Act & Assert
      assertThat(service.existsUserById(null)).isFalse();
      assertThat(service.existsUserById(USER_ID)).isTrue();
    }

    @Test
    @DisplayName("fetchAccountByUsername(username) -> account translated to the local record (AAA)")
    void accountTranslation() {
      // Arrange
      when(iamContextFacade.fetchAccountByUsername(USERNAME))
          .thenReturn(Optional.of(new AccountSummary(USER_ID, USERNAME, "ROLE_OWNER", "LOCAL")));
      doReturn(Optional.empty()).when(iamContextFacade).fetchAccountByUsername("ghost");

      // Act & Assert
      assertThat(service.fetchAccountByUsername(USERNAME))
          .contains(new ExternalIamService.Account(USER_ID, USERNAME, "ROLE_OWNER", "LOCAL"));
      assertThat(service.fetchAccountByUsername("ghost")).isEmpty();
    }
  }
}
