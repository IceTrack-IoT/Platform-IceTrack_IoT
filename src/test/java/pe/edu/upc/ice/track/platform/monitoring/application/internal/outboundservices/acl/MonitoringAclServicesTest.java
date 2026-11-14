package pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.AssetContextFacade;
import pe.edu.upc.ice.track.platform.assets.interfaces.acl.EquipmentSummary;
import pe.edu.upc.ice.track.platform.device.interfaces.acl.DeviceContextFacade;
import pe.edu.upc.ice.track.platform.device.interfaces.acl.LinkedDeviceCounts;
import pe.edu.upc.ice.track.platform.iam.interfaces.acl.IamContextFacade;
import pe.edu.upc.ice.track.platform.profiles.interfaces.acl.ProfilesContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.InProgressServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestContextFacade;
import pe.edu.upc.ice.track.platform.service.request.interfaces.acl.ServiceRequestStatusCount;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class MonitoringAclServicesTest {

  @Mock
  private AssetContextFacade assetContextFacade;
  @Mock
  private DeviceContextFacade deviceContextFacade;
  @Mock
  private IamContextFacade iamContextFacade;
  @Mock
  private ProfilesContextFacade profilesContextFacade;
  @Mock
  private ServiceRequestContextFacade serviceRequestContextFacade;

  @Test
  @DisplayName("ExternalAssetManagementService.fetchTemperatureThreshold(Long) -> delegates and returns the range (AAA)")
  void thresholdDelegates() {
    // Arrange
    var service = new ExternalAssetManagementService(assetContextFacade);
    when(assetContextFacade.getEquipmentThresholdRange(EQUIPMENT_ID)).thenReturn(Optional.of(new double[] {2.0, 8.0}));

    // Act & Assert
    assertThat(service.fetchTemperatureThreshold(EQUIPMENT_ID)).hasValueSatisfying(
        range -> assertThat(range).containsExactly(2.0, 8.0));
  }

  @Test
  @DisplayName("ExternalAssetManagementService -> null equipment ids are answered without calling the facade (AAA)")
  void nullEquipmentShortCircuits() {
    var service = new ExternalAssetManagementService(assetContextFacade);

    assertThat(service.fetchTemperatureThreshold(null)).isEmpty();
    assertThat(service.fetchOwnerId(null)).isEmpty();
    verifyNoInteractions(assetContextFacade);
  }

  @Test
  @DisplayName("ExternalAssetManagementService -> ownership, owner lookup, readings and connectivity delegate to the facade (AAA)")
  void simpleDelegations() {
    // Arrange
    var service = new ExternalAssetManagementService(assetContextFacade);
    when(assetContextFacade.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_PROFILE_ID)).thenReturn(true);
    when(assetContextFacade.getEquipmentOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(assetContextFacade.recordEquipmentReading(EQUIPMENT_ID, 5.0, T0, T0.plusSeconds(1))).thenReturn(true);
    when(assetContextFacade.markEquipmentOffline(EQUIPMENT_ID)).thenReturn(true);
    when(assetContextFacade.fetchOnlineEquipmentIds()).thenReturn(List.of(EQUIPMENT_ID));
    when(assetContextFacade.existsSiteForOwner(SITE_ID, OWNER_PROFILE_ID)).thenReturn(true);
    when(assetContextFacade.countSitesByOwner(OWNER_PROFILE_ID)).thenReturn(4);
    when(assetContextFacade.fetchEquipmentStatusNames()).thenReturn(List.of("ACTIVE"));

    // Act & Assert
    assertThat(service.isEquipmentOwnedBy(EQUIPMENT_ID, OWNER_PROFILE_ID)).isTrue();
    assertThat(service.fetchOwnerId(EQUIPMENT_ID)).contains(OWNER_PROFILE_ID);
    assertThat(service.recordEquipmentReading(EQUIPMENT_ID, 5.0, T0, T0.plusSeconds(1))).isTrue();
    assertThat(service.markEquipmentOffline(EQUIPMENT_ID)).isTrue();
    assertThat(service.fetchOnlineEquipmentIds()).containsExactly(EQUIPMENT_ID);
    assertThat(service.isSiteOwnedBy(SITE_ID, OWNER_PROFILE_ID)).isTrue();
    assertThat(service.countSitesOwnedBy(OWNER_PROFILE_ID)).isEqualTo(4);
    assertThat(service.fetchEquipmentStatusNames()).containsExactly("ACTIVE");
  }

  @Test
  @DisplayName("ExternalAssetManagementService.fetchEquipmentOwnedBy(Long, Long) -> maps summaries to MonitoredEquipment (AAA)")
  void equipmentOwnedByMapsSummaries() {
    // Arrange
    var service = new ExternalAssetManagementService(assetContextFacade);
    when(assetContextFacade.fetchEquipmentSummariesByOwner(OWNER_PROFILE_ID, SITE_ID)).thenReturn(List.of(
        new EquipmentSummary(EQUIPMENT_ID, "Fridge", "ACTIVE", SITE_ID, "Lima", T0)));

    // Act
    var result = service.fetchEquipmentOwnedBy(OWNER_PROFILE_ID, SITE_ID);

    // Assert
    assertThat(result).containsExactly(new MonitoredEquipment(EQUIPMENT_ID, "ACTIVE", T0));
  }

  @Test
  @DisplayName("ExternalAssetManagementService.fetchEquipmentOwnedBy(Long, Long) -> a null owner yields an empty list (AAA)")
  void equipmentOwnedByNullOwner() {
    var service = new ExternalAssetManagementService(assetContextFacade);

    assertThat(service.fetchEquipmentOwnedBy(null, null)).isEmpty();
    verifyNoInteractions(assetContextFacade);
  }

  @Test
  @DisplayName("ExternalDeviceServiceForMonitoring.fetchLinkedDeviceId(Long) -> returns the linked device (AAA)")
  void linkedDeviceFound() {
    var service = new ExternalDeviceServiceForMonitoring(deviceContextFacade);
    when(deviceContextFacade.fetchLinkedDeviceId(EQUIPMENT_ID)).thenReturn(DEVICE_ID);

    assertThat(service.fetchLinkedDeviceId(EQUIPMENT_ID)).contains(DEVICE_ID);
  }

  @Test
  @DisplayName("ExternalDeviceServiceForMonitoring.fetchLinkedDeviceId(Long) -> 0, null and a null equipment mean 'none' (AAA)")
  void linkedDeviceNotFound() {
    // Arrange
    var service = new ExternalDeviceServiceForMonitoring(deviceContextFacade);
    when(deviceContextFacade.fetchLinkedDeviceId(EQUIPMENT_ID)).thenReturn(0L);
    org.mockito.Mockito.doReturn(null).when(deviceContextFacade).fetchLinkedDeviceId(99L);

    // Act & Assert
    assertThat(service.fetchLinkedDeviceId(EQUIPMENT_ID)).isEmpty();
    assertThat(service.fetchLinkedDeviceId(99L)).isEmpty();
    assertThat(service.fetchLinkedDeviceId(null)).isEmpty();
  }

  @Test
  @DisplayName("ExternalDeviceServiceForMonitoring.recordDeviceActivity(Long, Instant) -> forwards the activity (AAA)")
  void recordActivityForwards() {
    var service = new ExternalDeviceServiceForMonitoring(deviceContextFacade);

    service.recordDeviceActivity(DEVICE_ID, T0);

    verify(deviceContextFacade, times(1)).recordActivity(DEVICE_ID, T0);
  }

  @Test
  @DisplayName("ExternalDeviceServiceForMonitoring.recordDeviceActivity(Long, Instant) -> ignores a null device or instant (AAA)")
  void recordActivityIgnoresNulls() {
    var service = new ExternalDeviceServiceForMonitoring(deviceContextFacade);

    service.recordDeviceActivity(null, T0);
    service.recordDeviceActivity(DEVICE_ID, null);

    verifyNoInteractions(deviceContextFacade);
  }

  @Test
  @DisplayName("ExternalDeviceServiceForMonitoring.countLinkedDevices(Collection) -> maps the counts (AAA)")
  void countLinkedDevices() {
    var service = new ExternalDeviceServiceForMonitoring(deviceContextFacade);
    when(deviceContextFacade.countLinkedDevices(List.of(EQUIPMENT_ID))).thenReturn(new LinkedDeviceCounts(3, 1));

    var totals = service.countLinkedDevices(List.of(EQUIPMENT_ID));

    assertThat(totals).isEqualTo(new ExternalDeviceServiceForMonitoring.LinkedDeviceTotals(3, 1));
  }

  @Test
  @DisplayName("ExternalProfilesServiceForMonitoring.fetchUserIdByUsername(String) -> returns the user id when found (AAA)")
  void userIdFound() {
    var service = new ExternalProfilesServiceForMonitoring(iamContextFacade, profilesContextFacade);
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);

    assertThat(service.fetchUserIdByUsername(USERNAME)).contains(USER_ID);
  }

  @Test
  @DisplayName("ExternalProfilesServiceForMonitoring.fetchUserIdByUsername(String) -> 0, null and blank usernames mean 'none' (AAA)")
  void userIdNotFound() {
    // Arrange
    var service = new ExternalProfilesServiceForMonitoring(iamContextFacade, profilesContextFacade);
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(0L);

    // Act & Assert
    assertThat(service.fetchUserIdByUsername(USERNAME)).isEmpty();
    assertThat(service.fetchUserIdByUsername(null)).isEmpty();
    assertThat(service.fetchUserIdByUsername("  ")).isEmpty();
    verify(iamContextFacade, times(1)).fetchUserIdByUsername(any());
  }

  @Test
  @DisplayName("ExternalProfilesServiceForMonitoring.fetchOwnerIdByUsername(String) -> resolves user then owner profile (AAA)")
  void ownerIdFound() {
    var service = new ExternalProfilesServiceForMonitoring(iamContextFacade, profilesContextFacade);
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(OWNER_PROFILE_ID);

    assertThat(service.fetchOwnerIdByUsername(USERNAME)).contains(OWNER_PROFILE_ID);
  }

  @Test
  @DisplayName("ExternalProfilesServiceForMonitoring.fetchOwnerIdByUsername(String) -> a user without owner profile yields empty (AAA)")
  void ownerIdNotFound() {
    var service = new ExternalProfilesServiceForMonitoring(iamContextFacade, profilesContextFacade);
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchOwnerIdByUserId(USER_ID)).thenReturn(0L);

    assertThat(service.fetchOwnerIdByUsername(USERNAME)).isEmpty();
  }

  @Test
  @DisplayName("ExternalProfilesServiceForMonitoring.fetchTechnicianIdByUsername(String) -> resolves user then technician profile (AAA)")
  void technicianIdFound() {
    var service = new ExternalProfilesServiceForMonitoring(iamContextFacade, profilesContextFacade);
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(USER_ID);
    when(profilesContextFacade.fetchTechnicianIdByUserId(USER_ID)).thenReturn(TECHNICIAN_PROFILE_ID);

    assertThat(service.fetchTechnicianIdByUsername(USERNAME)).contains(TECHNICIAN_PROFILE_ID);
  }

  @Test
  @DisplayName("ExternalProfilesServiceForMonitoring.fetchTechnicianIdByUsername(String) -> an unknown user never reaches profiles (AAA)")
  void technicianIdUnknownUser() {
    var service = new ExternalProfilesServiceForMonitoring(iamContextFacade, profilesContextFacade);
    when(iamContextFacade.fetchUserIdByUsername(USERNAME)).thenReturn(0L);

    assertThat(service.fetchTechnicianIdByUsername(USERNAME)).isEmpty();
    verify(profilesContextFacade, never()).fetchTechnicianIdByUserId(any());
  }

  @Test
  @DisplayName("ExternalServiceRequestServiceForMonitoring.hasActiveAssignmentOnEquipment(...) -> delegates (AAA)")
  void assignmentDelegates() {
    var service = new ExternalServiceRequestServiceForMonitoring(serviceRequestContextFacade);
    when(serviceRequestContextFacade.hasActiveAssignmentOnEquipment(TECHNICIAN_PROFILE_ID, EQUIPMENT_ID))
        .thenReturn(true);

    assertThat(service.hasActiveAssignmentOnEquipment(TECHNICIAN_PROFILE_ID, EQUIPMENT_ID)).isTrue();
  }

  @Test
  @DisplayName("ExternalServiceRequestServiceForMonitoring.findInProgressServiceRequestAt(...) -> maps to a ServiceRequestAttribution (AAA)")
  void attributionMapped() {
    // Arrange
    var service = new ExternalServiceRequestServiceForMonitoring(serviceRequestContextFacade);
    when(serviceRequestContextFacade.findInProgressServiceRequestAt(EQUIPMENT_ID, T0))
        .thenReturn(Optional.of(new InProgressServiceRequest(SERVICE_REQUEST_ID, TECHNICIAN_PROFILE_ID)));

    // Act & Assert
    assertThat(service.findInProgressServiceRequestAt(EQUIPMENT_ID, T0))
        .contains(new ServiceRequestAttribution(SERVICE_REQUEST_ID, TECHNICIAN_PROFILE_ID));
  }

  @Test
  @DisplayName("ExternalServiceRequestServiceForMonitoring.findInProgressServiceRequestAt(...) -> empty when no order was in progress (AAA)")
  void attributionAbsent() {
    var service = new ExternalServiceRequestServiceForMonitoring(serviceRequestContextFacade);
    when(serviceRequestContextFacade.findInProgressServiceRequestAt(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());

    assertThat(service.findInProgressServiceRequestAt(EQUIPMENT_ID, T0)).isEmpty();
  }

  @Test
  @DisplayName("ExternalServiceRequestServiceForMonitoring.countServiceRequests(Long, Long) -> totals per status and active count (AAA)")
  void countServiceRequests() {
    // Arrange
    var service = new ExternalServiceRequestServiceForMonitoring(serviceRequestContextFacade);
    when(serviceRequestContextFacade.countServiceRequestsByStatus(OWNER_PROFILE_ID, SITE_ID)).thenReturn(List.of(
        new ServiceRequestStatusCount("PENDING", true, 2),
        new ServiceRequestStatusCount("IN_PROGRESS", true, 3),
        new ServiceRequestStatusCount("COMPLETED", false, 7)));

    // Act
    var totals = service.countServiceRequests(OWNER_PROFILE_ID, SITE_ID);

    // Assert
    assertThat(totals.active()).isEqualTo(5);
    assertThat(totals.byStatus()).containsExactly(
        java.util.Map.entry("PENDING", 2L), java.util.Map.entry("IN_PROGRESS", 3L), java.util.Map.entry("COMPLETED", 7L));
  }
}
