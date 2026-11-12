package pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentSummary;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.EquipmentRef;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalEquipmentService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalMonitoringService;
import pe.edu.upc.ice.track.platform.service.request.application.internal.outboundservices.acl.ExternalUserProfileService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class ServiceRequestAclServicesTest {

  @Nested
  class Equipment {

    @Mock private AssetContextFacade assets;

    @Test
    @DisplayName("fetchOwnedEquipment(ownerId, equipmentId) -> EquipmentRef with the site when the owner owns the unit (AAA)")
    void owned() {
      // Arrange
      doReturn(true).when(assets).existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID);
      doReturn(Optional.of(SITE_ID)).when(assets).getSiteIdByEquipment(EQUIPMENT_ID);

      // Act
      var ref = new ExternalEquipmentService(assets).fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID);

      // Assert
      assertThat(ref).contains(new EquipmentRef(EQUIPMENT_ID, OWNER_ID, SITE_ID));
    }

    @Test
    @DisplayName("fetchOwnedEquipment(ownerId, equipmentId) -> empty when not owned, site unknown or an argument is null (AAA)")
    void notOwned() {
      // Arrange
      doReturn(false).when(assets).existsEquipmentForOwner(EQUIPMENT_ID, OTHER_OWNER_ID);
      doReturn(true).when(assets).existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID);
      doReturn(Optional.empty()).when(assets).getSiteIdByEquipment(EQUIPMENT_ID);
      var service = new ExternalEquipmentService(assets);

      // Act & Assert
      assertThat(service.fetchOwnedEquipment(OTHER_OWNER_ID, EQUIPMENT_ID)).isEmpty();
      assertThat(service.fetchOwnedEquipment(OWNER_ID, EQUIPMENT_ID)).isEmpty();
      assertThat(service.fetchOwnedEquipment(null, EQUIPMENT_ID)).isEmpty();
      assertThat(service.fetchOwnedEquipment(OWNER_ID, null)).isEmpty();
    }

    @Test
    @DisplayName("isEquipmentOwnedBy(ownerId, equipmentId) -> delegates; null arguments are false without asking (AAA)")
    void isOwned() {
      // Arrange
      doReturn(true).when(assets).existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID);
      var service = new ExternalEquipmentService(assets);

      // Act & Assert
      assertThat(service.isEquipmentOwnedBy(OWNER_ID, EQUIPMENT_ID)).isTrue();
      assertThat(service.isEquipmentOwnedBy(null, EQUIPMENT_ID)).isFalse();
      assertThat(service.isEquipmentOwnedBy(OWNER_ID, null)).isFalse();
      verify(assets, times(1)).existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID);
    }

    @Test
    @DisplayName("fetchEquipmentNamesByOwner(ownerId) -> id-to-name map skipping summaries without id or name (AAA)")
    void equipmentNames() {
      // Arrange
      doReturn(List.of(
          new EquipmentSummary(10L, "Freezer 1", "ACTIVE", 3L, "Lima", null),
          new EquipmentSummary(11L, null, "ACTIVE", 3L, "Lima", null),
          new EquipmentSummary(null, "Ghost", "ACTIVE", 3L, "Lima", null)))
          .when(assets).fetchEquipmentSummariesByOwner(OWNER_ID, null);

      // Act
      var names = new ExternalEquipmentService(assets).fetchEquipmentNamesByOwner(OWNER_ID);

      // Assert
      assertThat(names).containsOnlyKeys(10L).containsEntry(10L, "Freezer 1");
    }

    @Test
    @DisplayName("fetchEquipmentNamesByOwner / fetchSiteNamesByOwner(null) -> empty map without asking (AAA)")
    void nullOwner() {
      // Arrange
      var service = new ExternalEquipmentService(assets);

      // Act & Assert
      assertThat(service.fetchEquipmentNamesByOwner(null)).isEmpty();
      assertThat(service.fetchSiteNamesByOwner(null)).isEmpty();
      verifyNoInteractions(assets);
    }

    @Test
    @DisplayName("fetchSiteNamesByOwner(ownerId) -> the facade's map (AAA)")
    void siteNames() {
      // Arrange
      doReturn(Map.of(SITE_ID, "Lima")).when(assets).fetchSiteNamesByOwner(OWNER_ID);

      // Act & Assert
      assertThat(new ExternalEquipmentService(assets).fetchSiteNamesByOwner(OWNER_ID)).isEqualTo(Map.of(SITE_ID, "Lima"));
    }
  }

  @Nested
  class Monitoring {

    @Mock private MonitoringContextFacade monitoring;

    @Test
    @DisplayName("fetchAlertEquipmentId(alertId) -> the unit id; 0, null or null alert id -> empty (AAA)")
    void alertEquipment() {
      // Arrange
      doReturn(EQUIPMENT_ID).when(monitoring).fetchAlertEquipmentId(ALERT_ID);
      doReturn(0L).when(monitoring).fetchAlertEquipmentId(999L);
      doReturn(null).when(monitoring).fetchAlertEquipmentId(998L);
      var service = new ExternalMonitoringService(monitoring);

      // Act & Assert
      assertThat(service.fetchAlertEquipmentId(ALERT_ID)).contains(EQUIPMENT_ID);
      assertThat(service.fetchAlertEquipmentId(999L)).isEmpty();
      assertThat(service.fetchAlertEquipmentId(998L)).isEmpty();
      assertThat(service.fetchAlertEquipmentId(null)).isEmpty();
    }
  }

  @Nested
  class UserProfiles {

    @Mock private IamContextFacade iam;
    @Mock private ProfilesContextFacade profilesFacade;

    private ExternalUserProfileService service() {
      return new ExternalUserProfileService(iam, profilesFacade);
    }

    @Test
    @DisplayName("fetchOwnerIdByUsername / fetchTechnicianIdByUsername -> username resolved to user then to profile (AAA)")
    void resolves() {
      // Arrange
      doReturn(9L).when(iam).fetchUserIdByUsername(USERNAME);
      doReturn(OWNER_ID).when(profilesFacade).fetchOwnerIdByUserId(9L);
      doReturn(TECHNICIAN_ID).when(profilesFacade).fetchTechnicianIdByUserId(9L);

      // Act & Assert
      assertThat(service().fetchOwnerIdByUsername(USERNAME)).contains(OWNER_ID);
      assertThat(service().fetchTechnicianIdByUsername(USERNAME)).contains(TECHNICIAN_ID);
    }

    @Test
    @DisplayName("fetch*IdByUsername -> empty for blank/null username, unknown account (0) or missing profile (0) (AAA)")
    void emptyCases() {
      // Arrange
      doReturn(0L).when(iam).fetchUserIdByUsername("ghost");
      doReturn(9L).when(iam).fetchUserIdByUsername(USERNAME);
      doReturn(0L).when(profilesFacade).fetchOwnerIdByUserId(9L);
      doReturn(0L).when(profilesFacade).fetchTechnicianIdByUserId(9L);

      // Act & Assert
      assertThat(service().fetchOwnerIdByUsername(null)).isEmpty();
      assertThat(service().fetchOwnerIdByUsername("  ")).isEmpty();
      assertThat(service().fetchTechnicianIdByUsername("")).isEmpty();
      assertThat(service().fetchOwnerIdByUsername("ghost")).isEmpty();
      assertThat(service().fetchOwnerIdByUsername(USERNAME)).isEmpty();
      assertThat(service().fetchTechnicianIdByUsername(USERNAME)).isEmpty();
    }

    @Test
    @DisplayName("isTechnicianEligibleForAssignment(id) -> delegates; null is false without asking (AAA)")
    void eligibility() {
      // Arrange
      doReturn(true).when(profilesFacade).isTechnicianEligibleForAssignment(TECHNICIAN_ID);
      doReturn(false).when(profilesFacade).isTechnicianEligibleForAssignment(OTHER_TECHNICIAN_ID);

      // Act & Assert
      assertThat(service().isTechnicianEligibleForAssignment(TECHNICIAN_ID)).isTrue();
      assertThat(service().isTechnicianEligibleForAssignment(OTHER_TECHNICIAN_ID)).isFalse();
      assertThat(service().isTechnicianEligibleForAssignment(null)).isFalse();
    }

    @Test
    @DisplayName("fetchTechnicianFullNames(ids) -> the facade's map; null or empty ids -> empty map without asking (AAA)")
    void names() {
      // Arrange
      var ids = Set.of(TECHNICIAN_ID);
      doReturn(Map.of(TECHNICIAN_ID, "Luis Rojas")).when(profilesFacade).fetchTechnicianFullNames(ids);

      // Act & Assert
      assertThat(service().fetchTechnicianFullNames(ids)).isEqualTo(Map.of(TECHNICIAN_ID, "Luis Rojas"));
      assertThat(service().fetchTechnicianFullNames(null)).isEmpty();
      assertThat(service().fetchTechnicianFullNames(Set.of())).isEmpty();
      verify(profilesFacade, times(1)).fetchTechnicianFullNames(ids);
    }
  }
}
