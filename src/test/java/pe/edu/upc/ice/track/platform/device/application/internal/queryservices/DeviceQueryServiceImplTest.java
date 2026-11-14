package pe.edu.upc.ice.track.platform.device.application.internal.queryservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice.PairedEquipment;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalMonitoringServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DevicePage;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceCommandFixtures;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class DeviceQueryServiceImplTest {

  private static final String EQUIPMENT_NAME = "Refrigerador 1";
  private static final Duration THREE_MINUTES = Duration.ofMinutes(3);

  @Mock
  private DeviceRepository deviceRepository;
  @Mock
  private ExternalAssetManagementServiceForDevice externalAssetManagementService;
  @Mock
  private DeviceExternalOwnerService deviceExternalOwnerService;
  @Mock
  private DeviceCredentialService deviceCredentialService;
  @Mock
  private ExternalMonitoringServiceForDevice externalMonitoringService;

  @InjectMocks
  private DeviceQueryServiceImpl service;


  private void stubOwner() {
    when(deviceExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
  }

  private void stubNoOwnerProfile() {
    when(deviceExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
  }

  private static PairedEquipment pairedEquipment() {
    return new PairedEquipment(EQUIPMENT_ID, EQUIPMENT_NAME, SITE_ID);
  }

  private void stubOwnedEquipment(PairedEquipment... equipment) {
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_ID, null)).thenReturn(List.of(equipment));
  }

  private static Device pairedSeenAt(Long deviceId, String uid, Long equipmentId, Instant seenAt) {
    return DeviceDomainFixtures.deviceBuilder().id(deviceId).uid(uid).paired().equipmentId(equipmentId)
        .lastSeenAt(seenAt).build();
  }

  @Test
  @DisplayName("handle(GetDeviceByIdQuery) -> returns the details of a device on the owner's equipment (AAA)")
  void handleByIdOwnedDevice() {
    // Arrange
    var device = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
    stubOwner();
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(true);
    when(externalAssetManagementService.fetchPairedEquipment(EQUIPMENT_ID)).thenReturn(Optional.of(pairedEquipment()));
    when(externalMonitoringService.fetchOfflineWindows(List.of(EQUIPMENT_ID))).thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var result = service.handle(DeviceCommandFixtures.deviceByIdQuery());

    // Assert
    assertThat(result).isPresent();
    var details = result.get();
    assertThat(details.device()).isSameAs(device);
    assertThat(details.pairedEquipmentName()).isEqualTo(EQUIPMENT_NAME);
    assertThat(details.pairedSiteId()).isEqualTo(SITE_ID);
    assertThat(details.connectivity().state()).isEqualTo(DeviceConnectivityState.REPORTING);
    assertThat(details.connectivity().silentAfter()).isEqualTo(THREE_MINUTES);
  }

  @Test
  @DisplayName("handle(GetDeviceByIdQuery) -> returns empty when the device does not exist (AAA)")
  void handleByIdMissingDevice() {
    // Arrange
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(DeviceCommandFixtures.deviceByIdQuery());

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(deviceExternalOwnerService, externalAssetManagementService, externalMonitoringService);
  }

  @Test
  @DisplayName("handle(GetDeviceByIdQuery) -> returns empty when the caller holds no owner profile (AAA)")
  void handleByIdNoOwnerProfile() {
    // Arrange
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.pairedDevice()));
    stubNoOwnerProfile();

    // Act
    var result = service.handle(DeviceCommandFixtures.deviceByIdQuery());

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(externalAssetManagementService, externalMonitoringService);
  }

  @Test
  @DisplayName("handle(GetDeviceByIdQuery) -> returns empty for a device that belongs to another owner (AAA)")
  void handleByIdForeignDevice() {
    // Arrange
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.pairedDevice()));
    stubOwner();
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.deviceByIdQuery());

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(externalMonitoringService);
  }

  @Test
  @DisplayName("handle(GetDeviceByIdQuery) -> returns empty for a device linked to no equipment without asking assets (AAA)")
  void handleByIdUnlinkedDevice() {
    // Arrange
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.unpairedDevice()));
    stubOwner();

    // Act
    var result = service.handle(DeviceCommandFixtures.deviceByIdQuery());

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(externalAssetManagementService);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> SITE_NOT_FOUND when the site filter is not the owner's (AAA)")
  void handleDevicesForeignSite() {
    // Arrange
    stubOwner();
    when(externalAssetManagementService.existsSiteForOwner(SITE_ID, OWNER_ID)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery(null, SITE_ID));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("SITE_NOT_FOUND");
    assertThat(error.message()).contains(String.valueOf(SITE_ID));
    verifyNoInteractions(deviceRepository);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> EQUIPMENT_NOT_FOUND when the equipment filter is not the owner's (AAA)")
  void handleDevicesForeignEquipment() {
    // Arrange
    stubOwner();
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery(EQUIPMENT_ID, null));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("EQUIPMENT_NOT_FOUND");
    verifyNoInteractions(deviceRepository);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> SITE_NOT_FOUND for a caller with no owner profile naming a site (AAA)")
  void handleDevicesSiteWithoutOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();
    when(externalAssetManagementService.existsSiteForOwner(SITE_ID, null)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery(null, SITE_ID));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("SITE_NOT_FOUND");
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> unpaged listing describes every device with its equipment and connectivity (AAA)")
  void handleDevicesUnpaged() {
    // Arrange
    var reporting = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    var silent = pairedSeenAt(OTHER_DEVICE_ID, OTHER_DEVICE_UID, OTHER_EQUIPMENT_ID, Instant.now().minus(Duration.ofHours(1)));
    stubOwner();
    stubOwnedEquipment(pairedEquipment(), new PairedEquipment(OTHER_EQUIPMENT_ID, "Cámara 2", SITE_ID));
    when(deviceRepository.findAllLinkedToEquipment(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID), Set.of(), null))
        .thenReturn(List.of(reporting, silent));
    when(externalMonitoringService.fetchOfflineWindows(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID)))
        .thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES, OTHER_EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery());

    // Assert
    var page = ResultTestUtils.successValue(result);
    assertThat(page.content()).hasSize(2);
    assertThat(page.content().get(0).pairedEquipmentName()).isEqualTo(EQUIPMENT_NAME);
    assertThat(page.content().get(0).connectivity().state()).isEqualTo(DeviceConnectivityState.REPORTING);
    assertThat(page.content().get(1).pairedEquipmentName()).isEqualTo("Cámara 2");
    assertThat(page.content().get(1).connectivity().state()).isEqualTo(DeviceConnectivityState.SILENT);
    assertThat(page.page()).isZero();
    assertThat(page.totalElements()).isEqualTo(2);
    verify(deviceRepository, never()).findPageLinkedToEquipment(
        org.mockito.ArgumentMatchers.anyCollection(), org.mockito.ArgumentMatchers.anySet(),
        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt());
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> paged listing delegates paging, statuses and search to the repository (AAA)")
  void handleDevicesPaged() {
    // Arrange
    var device = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    var statuses = Set.of(DeviceStatus.PAIRED);
    stubOwner();
    stubOwnedEquipment(pairedEquipment());
    when(deviceRepository.findPageLinkedToEquipment(Set.of(EQUIPMENT_ID), statuses, "esp", 2, 10))
        .thenReturn(new DevicePage(List.of(device), 2, 10, 21));
    when(externalMonitoringService.fetchOfflineWindows(Set.of(EQUIPMENT_ID))).thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var result = service.handle(DeviceCommandFixtures.pagedDevicesQuery(statuses, "  esp ", 2, 10));

    // Assert
    var page = ResultTestUtils.successValue(result);
    assertThat(page.content()).hasSize(1);
    assertThat(page.page()).isEqualTo(2);
    assertThat(page.size()).isEqualTo(10);
    assertThat(page.totalElements()).isEqualTo(21);
    assertThat(page.totalPages()).isEqualTo(3);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> an empty page never asks monitoring for offline windows (AAA)")
  void handleDevicesEmptyPage() {
    // Arrange
    stubOwner();
    stubOwnedEquipment(pairedEquipment());
    when(deviceRepository.findAllLinkedToEquipment(Set.of(EQUIPMENT_ID), Set.of(), null)).thenReturn(List.of());

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery());

    // Assert
    var page = ResultTestUtils.successValue(result);
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isZero();
    assertThat(page.totalPages()).isEqualTo(1);
    verifyNoInteractions(externalMonitoringService);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> narrows the scope to the requested equipment when it is the owner's (AAA)")
  void handleDevicesEquipmentFilter() {
    // Arrange
    var device = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    stubOwner();
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(true);
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_ID, null))
        .thenReturn(List.of(pairedEquipment(), new PairedEquipment(OTHER_EQUIPMENT_ID, "Cámara 2", SITE_ID)));
    when(deviceRepository.findAllLinkedToEquipment(Set.of(EQUIPMENT_ID), Set.of(), null)).thenReturn(List.of(device));
    when(externalMonitoringService.fetchOfflineWindows(Set.of(EQUIPMENT_ID))).thenReturn(Map.of());

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery(EQUIPMENT_ID, null));

    // Assert
    assertThat(ResultTestUtils.successValue(result).content()).hasSize(1);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> uses an empty scope when the equipment is outside the site-filtered ownership (AAA)")
  void handleDevicesEquipmentOutsideSiteScope() {
    // Arrange
    stubOwner();
    when(externalAssetManagementService.existsSiteForOwner(SITE_ID, OWNER_ID)).thenReturn(true);
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(true);
    when(externalAssetManagementService.fetchEquipmentOwnedBy(OWNER_ID, SITE_ID)).thenReturn(List.of());
    when(deviceRepository.findAllLinkedToEquipment(Set.of(), Set.of(), null)).thenReturn(List.of());

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery(EQUIPMENT_ID, SITE_ID));

    // Assert
    assertThat(ResultTestUtils.successValue(result).content()).isEmpty();
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> a caller with no owner profile and no filters gets an empty listing (AAA)")
  void handleDevicesNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();
    when(externalAssetManagementService.fetchEquipmentOwnedBy(null, null)).thenReturn(List.of());
    when(deviceRepository.findAllLinkedToEquipment(Set.of(), Set.of(), null)).thenReturn(List.of());

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQuery());

    // Assert
    assertThat(ResultTestUtils.successValue(result).content()).isEmpty();
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> connectivity filter keeps only the matching devices, unpaged (AAA)")
  void handleDevicesConnectivityFilterUnpaged() {
    // Arrange
    var reporting = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    var silent = pairedSeenAt(OTHER_DEVICE_ID, OTHER_DEVICE_UID, OTHER_EQUIPMENT_ID, Instant.now().minus(Duration.ofHours(1)));
    stubOwner();
    stubOwnedEquipment(pairedEquipment(), new PairedEquipment(OTHER_EQUIPMENT_ID, "Cámara 2", SITE_ID));
    when(deviceRepository.findAllLinkedToEquipment(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID), Set.of(), null))
        .thenReturn(List.of(reporting, silent));
    when(externalMonitoringService.fetchOfflineWindows(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID)))
        .thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES, OTHER_EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQueryByConnectivity(Set.of(DeviceConnectivityState.SILENT), null, null));

    // Assert
    var page = ResultTestUtils.successValue(result);
    assertThat(page.content()).hasSize(1);
    assertThat(page.content().get(0).device().getDeviceId()).isEqualTo(OTHER_DEVICE_ID);
    assertThat(page.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> connectivity filter slices the requested page in memory (AAA)")
  void handleDevicesConnectivityFilterPaged() {
    // Arrange
    var first = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    var second = pairedSeenAt(OTHER_DEVICE_ID, OTHER_DEVICE_UID, OTHER_EQUIPMENT_ID, Instant.now());
    stubOwner();
    stubOwnedEquipment(pairedEquipment(), new PairedEquipment(OTHER_EQUIPMENT_ID, "Cámara 2", SITE_ID));
    when(deviceRepository.findAllLinkedToEquipment(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID), Set.of(), null))
        .thenReturn(List.of(first, second));
    when(externalMonitoringService.fetchOfflineWindows(Set.of(EQUIPMENT_ID, OTHER_EQUIPMENT_ID)))
        .thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES, OTHER_EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQueryByConnectivity(
        Set.of(DeviceConnectivityState.REPORTING), 1, 1));

    // Assert
    var page = ResultTestUtils.successValue(result);
    assertThat(page.content()).hasSize(1);
    assertThat(page.content().get(0).device().getDeviceId()).isEqualTo(OTHER_DEVICE_ID);
    assertThat(page.page()).isEqualTo(1);
    assertThat(page.size()).isEqualTo(1);
    assertThat(page.totalElements()).isEqualTo(2);
    assertThat(page.totalPages()).isEqualTo(2);
  }

  @Test
  @DisplayName("handle(GetDevicesQuery) -> connectivity filter returns an empty slice for a page past the end (AAA)")
  void handleDevicesConnectivityFilterPageBeyondEnd() {
    // Arrange
    var device = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now());
    stubOwner();
    stubOwnedEquipment(pairedEquipment());
    when(deviceRepository.findAllLinkedToEquipment(Set.of(EQUIPMENT_ID), Set.of(), null)).thenReturn(List.of(device));
    when(externalMonitoringService.fetchOfflineWindows(Set.of(EQUIPMENT_ID))).thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var result = service.handle(DeviceCommandFixtures.devicesQueryByConnectivity(
        Set.of(DeviceConnectivityState.REPORTING), 5, 10));

    // Assert
    var page = ResultTestUtils.successValue(result);
    assertThat(page.content()).isEmpty();
    assertThat(page.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("describe(Device) -> an unpaired device has no equipment, skips monitoring and is UNPAIRED (AAA)")
  void describeUnpairedDevice() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();
    when(externalAssetManagementService.fetchPairedEquipment(null)).thenReturn(Optional.empty());

    // Act
    var details = service.describe(device);

    // Assert
    assertThat(details.device()).isSameAs(device);
    assertThat(details.pairedEquipmentName()).isNull();
    assertThat(details.pairedSiteId()).isNull();
    assertThat(details.connectivity().state()).isEqualTo(DeviceConnectivityState.UNPAIRED);
    verifyNoInteractions(externalMonitoringService);
  }

  @Test
  @DisplayName("describe(Device) -> a paired device that never reported is NEVER_REPORTED (AAA)")
  void describePairedNeverReported() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();
    when(externalAssetManagementService.fetchPairedEquipment(EQUIPMENT_ID)).thenReturn(Optional.of(pairedEquipment()));
    when(externalMonitoringService.fetchOfflineWindows(List.of(EQUIPMENT_ID))).thenReturn(Map.of(EQUIPMENT_ID, THREE_MINUTES));

    // Act
    var details = service.describe(device);

    // Assert
    assertThat(details.pairedEquipmentName()).isEqualTo(EQUIPMENT_NAME);
    assertThat(details.connectivity().state()).isEqualTo(DeviceConnectivityState.NEVER_REPORTED);
  }

  @Test
  @DisplayName("describe(Device) -> a revoked device is KEY_REVOKED (AAA)")
  void describeRevokedDevice() {
    // Arrange
    when(externalAssetManagementService.fetchPairedEquipment(EQUIPMENT_ID)).thenReturn(Optional.of(pairedEquipment()));
    when(externalMonitoringService.fetchOfflineWindows(List.of(EQUIPMENT_ID))).thenReturn(Map.of());

    // Act
    var details = service.describe(DeviceDomainFixtures.revokedDevice());

    // Assert
    assertThat(details.connectivity().state()).isEqualTo(DeviceConnectivityState.KEY_REVOKED);
  }

  @Test
  @DisplayName("describe(Device) -> without a known window a device that reported is REPORTING (AAA)")
  void describeWithoutOfflineWindow() {
    // Arrange
    var device = pairedSeenAt(DEVICE_ID, DeviceDomainFixtures.VALID_DEVICE_UID, EQUIPMENT_ID, Instant.now().minus(Duration.ofDays(1)));
    when(externalAssetManagementService.fetchPairedEquipment(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(externalMonitoringService.fetchOfflineWindows(List.of(EQUIPMENT_ID))).thenReturn(Map.of());

    // Act
    var details = service.describe(device);

    // Assert
    assertThat(details.pairedEquipmentName()).isNull();
    assertThat(details.connectivity().state()).isEqualTo(DeviceConnectivityState.REPORTING);
    assertThat(details.connectivity().silentAfter()).isNull();
  }

  @Test
  @DisplayName("authenticateByApiKey(String) -> returns the device and equipment of a valid, matching key (AAA)")
  void authenticateSuccess() {
    // Arrange
    when(deviceCredentialService.digestOf(VALID_API_KEY)).thenReturn(Optional.of(VALID_DIGEST));
    when(deviceRepository.findByApiKeyDigest(VALID_DIGEST)).thenReturn(Optional.of(DeviceDomainFixtures.pairedDevice()));
    when(deviceCredentialService.matches(VALID_API_KEY, VALID_DIGEST)).thenReturn(true);

    // Act
    var result = service.authenticateByApiKey(VALID_API_KEY);

    // Assert
    assertThat(result).isPresent();
    assertThat(result.get().deviceId()).isEqualTo(DEVICE_ID);
    assertThat(result.get().equipmentId()).isEqualTo(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("authenticateByApiKey(String) -> empty for a malformed key without touching the repository (AAA)")
  void authenticateMalformedKey() {
    // Arrange
    when(deviceCredentialService.digestOf("garbage")).thenReturn(Optional.empty());

    // Act
    var result = service.authenticateByApiKey("garbage");

    // Assert
    assertThat(result).isEmpty();
    verifyNoInteractions(deviceRepository);
  }

  @Test
  @DisplayName("authenticateByApiKey(String) -> empty when no device holds the digest (AAA)")
  void authenticateUnknownDigest() {
    // Arrange
    when(deviceCredentialService.digestOf(VALID_API_KEY)).thenReturn(Optional.of(VALID_DIGEST));
    when(deviceRepository.findByApiKeyDigest(VALID_DIGEST)).thenReturn(Optional.empty());

    // Act
    var result = service.authenticateByApiKey(VALID_API_KEY);

    // Assert
    assertThat(result).isEmpty();
    verify(deviceCredentialService, never()).matches(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
  }

  @Test
  @DisplayName("authenticateByApiKey(String) -> empty when the constant time comparison fails (AAA)")
  void authenticateDigestMismatch() {
    // Arrange
    when(deviceCredentialService.digestOf(VALID_API_KEY)).thenReturn(Optional.of(VALID_DIGEST));
    when(deviceRepository.findByApiKeyDigest(VALID_DIGEST)).thenReturn(Optional.of(DeviceDomainFixtures.pairedDevice()));
    when(deviceCredentialService.matches(VALID_API_KEY, VALID_DIGEST)).thenReturn(false);

    // Act
    var result = service.authenticateByApiKey(VALID_API_KEY);

    // Assert
    assertThat(result).isEmpty();
    verify(deviceCredentialService, times(1)).matches(VALID_API_KEY, VALID_DIGEST);
  }
}
