package pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalProfilesServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalServiceRequestServiceForMonitoring;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.CallerRole;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EquipmentAccessPolicyTest {

  @Mock
  private ExternalAssetManagementService assets;
  @Mock
  private ExternalProfilesServiceForMonitoring profiles;
  @Mock
  private ExternalServiceRequestServiceForMonitoring serviceRequests;

  @InjectMocks
  private EquipmentAccessPolicy policy;


  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> a null equipment id is never readable (AAA)")
  void canReadNullEquipment() {
    assertThat(policy.canRead(null, USERNAME, CallerRole.OWNER)).isFalse();
    verifyNoInteractions(assets, profiles, serviceRequests);
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> an owner reads the equipment it owns (AAA)")
  void ownerReadsOwnEquipment() {
    // Arrange
    when(profiles.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(assets.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(true);

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).isTrue();
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> an owner cannot read equipment it does not own (AAA)")
  void ownerCannotReadForeignEquipment() {
    // Arrange
    when(profiles.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(assets.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(false);

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).isFalse();
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> a user without owner profile cannot read (AAA)")
  void userWithoutOwnerProfile() {
    // Arrange
    when(profiles.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.OWNER)).isFalse();
    verifyNoInteractions(assets);
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> a technician reads equipment with an active assignment (AAA)")
  void technicianWithAssignment() {
    // Arrange
    when(profiles.fetchTechnicianIdByUsername(USERNAME)).thenReturn(Optional.of(TECHNICIAN_PROFILE_ID));
    when(serviceRequests.hasActiveAssignmentOnEquipment(TECHNICIAN_PROFILE_ID, EQUIPMENT_ID)).thenReturn(true);

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.TECHNICIAN)).isTrue();
    verifyNoInteractions(assets);
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> a technician without an assignment cannot read (AAA)")
  void technicianWithoutAssignment() {
    // Arrange
    when(profiles.fetchTechnicianIdByUsername(USERNAME)).thenReturn(Optional.of(TECHNICIAN_PROFILE_ID));
    when(serviceRequests.hasActiveAssignmentOnEquipment(TECHNICIAN_PROFILE_ID, EQUIPMENT_ID)).thenReturn(false);

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.TECHNICIAN)).isFalse();
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> a caller without technician profile cannot read as technician (AAA)")
  void technicianWithoutProfile() {
    // Arrange
    when(profiles.fetchTechnicianIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, CallerRole.TECHNICIAN)).isFalse();
    verifyNoInteractions(serviceRequests);
  }

  @Test
  @DisplayName("canRead(Long, String, CallerRole) -> a null role is treated as an owner check (AAA)")
  void nullRoleIsOwnerCheck() {
    // Arrange
    when(profiles.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(assets.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(true);

    // Act & Assert
    assertThat(policy.canRead(EQUIPMENT_ID, USERNAME, null)).isTrue();
  }


  @Test
  @DisplayName("canManage(Long, String) -> only the owner of the equipment may manage it (AAA)")
  void canManageOwner() {
    // Arrange
    when(profiles.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(assets.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(true);

    // Act & Assert
    assertThat(policy.canManage(EQUIPMENT_ID, USERNAME)).isTrue();
  }

  @Test
  @DisplayName("canManage(Long, String) -> a null equipment id or a non-owner cannot manage (AAA)")
  void canManageDenied() {
    // Arrange
    when(profiles.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());

    // Act & Assert
    assertThat(policy.canManage(null, USERNAME)).isFalse();
    assertThat(policy.canManage(EQUIPMENT_ID, USERNAME)).isFalse();
  }
}
