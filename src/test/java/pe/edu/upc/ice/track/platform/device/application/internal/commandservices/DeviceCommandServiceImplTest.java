package pe.edu.upc.ice.track.platform.device.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.DeviceExternalOwnerService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.acl.ExternalAssetManagementServiceForDevice;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.DeviceCredentialService;
import pe.edu.upc.ice.track.platform.device.application.internal.outboundservices.credentials.IssuedDeviceCredential;
import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DevicePairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceStatusChangedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceUnpairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.domain.repositories.DeviceRepository;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceCommandFixtures;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures;
import pe.edu.upc.ice.track.platform.shared.test.util.ResultTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class DeviceCommandServiceImplTest {

  @Mock
  private DeviceRepository deviceRepository;
  @Mock
  private ExternalAssetManagementServiceForDevice externalAssetManagementService;
  @Mock
  private DeviceExternalOwnerService deviceExternalOwnerService;
  @Mock
  private DeviceCredentialService deviceCredentialService;

  @InjectMocks
  private DeviceCommandServiceImpl service;

  // ---------- Helpers ----------

  private void stubOwner() {
    when(deviceExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
  }

  private void stubNoOwnerProfile() {
    when(deviceExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.empty());
  }

  private void stubOwnedEquipment() {
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(true);
  }

  private void stubIssuedCredential(String digest) {
    when(deviceCredentialService.issue()).thenReturn(new IssuedDeviceCredential(VALID_API_KEY, digest));
  }

  private void stubSaveReturnsArgument() {
    when(deviceRepository.save(any(Device.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  /** Stubs the whole ownership chain so that {@code device} is reachable by the caller. */
  private void stubReachableDevice(Device device) {
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));
    stubOwnedEquipment();
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> creates a new device, pairs it and returns the plain API key once (AAA)")
  void handleRegisterNewDevice() {
    // Arrange
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.existsLinkedToEquipment(EQUIPMENT_ID)).thenReturn(false);
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.empty());
    stubIssuedCredential(VALID_DIGEST);
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    var credential = ResultTestUtils.successValue(result);
    assertThat(credential.apiKey()).isEqualTo(VALID_API_KEY);
    var device = credential.device();
    assertThat(device.getDeviceUid()).isEqualTo(VALID_DEVICE_UID);
    assertThat(device.getBoardModel()).isEqualTo(VALID_BOARD_MODEL);
    assertThat(device.getFirmwareVersion()).isEqualTo(VALID_FIRMWARE_VERSION);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    assertThat(device.getPairedEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(device.getApiKeyDigest()).isEqualTo(VALID_DIGEST);
    verify(deviceRepository, times(2)).save(any(Device.class));
    verify(deviceCredentialService, times(1)).issue();
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> stores only the digest, never the plain API key (AAA)")
  void handleRegisterStoresOnlyTheDigest() {
    // Arrange
    var captor = ArgumentCaptor.forClass(Device.class);
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.empty());
    stubIssuedCredential(VALID_DIGEST);
    stubSaveReturnsArgument();

    // Act
    service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    verify(deviceRepository, times(2)).save(captor.capture());
    var persisted = captor.getValue();
    assertThat(persisted.getApiKeyDigest()).isEqualTo(VALID_DIGEST).isNotEqualTo(VALID_API_KEY);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> registers a PAIRED event on the device (AAA)")
  void handleRegisterRaisesPairedEvent() {
    // Arrange
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.empty());
    stubIssuedCredential(VALID_DIGEST);
    stubSaveReturnsArgument();

    // Act
    var device = ResultTestUtils.successValue(service.handle(DeviceCommandFixtures.registerDeviceCommand())).device();

    // Assert
    assertThat(device.domainEvents()).hasAtLeastOneElementOfType(DevicePairedEvent.class);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> reuses a known unpaired device, refreshing its hardware description (AAA)")
  void handleRegisterReusesUnpairedDevice() {
    // Arrange
    var existing = DeviceDomainFixtures.unpairedDevice();
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.of(existing));
    stubIssuedCredential(VALID_DIGEST);
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand(
        EQUIPMENT_ID, VALID_DEVICE_UID, DeviceDomainFixtures.UPDATED_BOARD_MODEL, DeviceDomainFixtures.UPDATED_FIRMWARE_VERSION));

    // Assert
    var device = ResultTestUtils.successValue(result).device();
    assertThat(device).isSameAs(existing);
    assertThat(device.getBoardModel()).isEqualTo(DeviceDomainFixtures.UPDATED_BOARD_MODEL);
    assertThat(device.getFirmwareVersion()).isEqualTo(DeviceDomainFixtures.UPDATED_FIRMWARE_VERSION);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    verify(deviceRepository, times(1)).save(existing);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleRegisterNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(deviceRepository, externalAssetManagementService, deviceCredentialService);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> EQUIPMENT_NOT_FOUND when the equipment is not the owner's (AAA)")
  void handleRegisterForeignEquipment() {
    // Arrange
    stubOwner();
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("EQUIPMENT_NOT_FOUND");
    assertThat(error.message()).contains(String.valueOf(EQUIPMENT_ID));
    verifyNoInteractions(deviceRepository, deviceCredentialService);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> DEVICE_CONFLICT when the equipment already has a device (AAA)")
  void handleRegisterEquipmentAlreadyPaired() {
    // Arrange
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.existsLinkedToEquipment(EQUIPMENT_ID)).thenReturn(true);

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("DEVICE_CONFLICT");
    assertThat(error.details()).isEqualTo(DeviceCommandServiceImpl.EQUIPMENT_ALREADY_PAIRED);
    verify(deviceRepository, never()).save(any(Device.class));
    verifyNoInteractions(deviceCredentialService);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> DEVICE_CONFLICT when the device uid is already linked to equipment (AAA)")
  void handleRegisterDeviceAlreadyLinked() {
    // Arrange
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.existsLinkedToEquipment(EQUIPMENT_ID)).thenReturn(false);
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.of(DeviceDomainFixtures.revokedDevice()));

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("DEVICE_CONFLICT");
    assertThat(error.details()).isEqualTo(DeviceCommandServiceImpl.DEVICE_ALREADY_PAIRED);
    verify(deviceRepository, never()).save(any(Device.class));
    verifyNoInteractions(deviceCredentialService);
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> VALIDATION_ERROR when the board model is blank, saving nothing (AAA)")
  void handleRegisterBlankBoardModel() {
    // Arrange
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(
        DeviceCommandFixtures.registerDeviceCommand(EQUIPMENT_ID, VALID_DEVICE_UID, "  ", VALID_FIRMWARE_VERSION));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("Board model");
    verify(deviceRepository, never()).save(any(Device.class));
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> VALIDATION_ERROR when the device uid exceeds 64 characters (AAA)")
  void handleRegisterTooLongUid() {
    // Arrange
    var tooLong = "U".repeat(Device.MAX_DEVICE_UID_LENGTH + 1);
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.findByDeviceUid(tooLong)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(
        DeviceCommandFixtures.registerDeviceCommand(EQUIPMENT_ID, tooLong, VALID_BOARD_MODEL, VALID_FIRMWARE_VERSION));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("VALIDATION_ERROR");
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> VALIDATION_ERROR when the credential service issues a malformed digest (AAA)")
  void handleRegisterMalformedDigest() {
    // Arrange
    stubOwner();
    stubOwnedEquipment();
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.empty());
    stubIssuedCredential("not-a-digest");
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.registerDeviceCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
    assertThat(error.details()).contains("API key digest");
  }

  @Test
  @DisplayName("handle(RegisterDeviceCommand) -> VALIDATION_ERROR when the equipment id is not positive (AAA)")
  void handleRegisterNonPositiveEquipmentId() {
    // Arrange
    when(deviceExternalOwnerService.fetchOwnerIdByUsername(USERNAME)).thenReturn(Optional.of(OWNER_ID));
    when(externalAssetManagementService.existsEquipmentForOwner(-1L, OWNER_ID)).thenReturn(true);
    when(deviceRepository.findByDeviceUid(VALID_DEVICE_UID)).thenReturn(Optional.empty());
    stubIssuedCredential(VALID_DIGEST);
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(
        DeviceCommandFixtures.registerDeviceCommand(-1L, VALID_DEVICE_UID, VALID_BOARD_MODEL, VALID_FIRMWARE_VERSION));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("VALIDATION_ERROR");
  }

  // ====================================================================================
  // handle(UnpairDeviceCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(UnpairDeviceCommand) -> unpairs a paired device, clearing equipment and credential (AAA)")
  void handleUnpairPairedDevice() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();
    stubReachableDevice(device);
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.unpairDeviceCommand());

    // Assert
    var unpaired = ResultTestUtils.successValue(result);
    assertThat(unpaired.getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
    assertThat(unpaired.getPairedEquipmentId()).isNull();
    assertThat(unpaired.getApiKeyDigest()).isNull();
    assertThat(unpaired.domainEvents()).hasAtLeastOneElementOfType(DeviceUnpairedEvent.class);
    verify(deviceRepository, times(1)).save(device);
  }

  @Test
  @DisplayName("handle(UnpairDeviceCommand) -> unpairs a revoked device too (AAA)")
  void handleUnpairRevokedDevice() {
    // Arrange
    stubReachableDevice(DeviceDomainFixtures.revokedDevice());
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.unpairDeviceCommand());

    // Assert
    assertThat(ResultTestUtils.successValue(result).getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
  }

  @Test
  @DisplayName("handle(UnpairDeviceCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleUnpairNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(DeviceCommandFixtures.unpairDeviceCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(deviceRepository);
  }

  @Test
  @DisplayName("handle(UnpairDeviceCommand) -> DEVICE_NOT_FOUND when the device does not exist (AAA)")
  void handleUnpairMissingDevice() {
    // Arrange
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(DeviceCommandFixtures.unpairDeviceCommand());

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("DEVICE_NOT_FOUND");
    assertThat(error.message()).contains(String.valueOf(DEVICE_ID));
    verify(deviceRepository, never()).save(any(Device.class));
  }

  @Test
  @DisplayName("handle(UnpairDeviceCommand) -> DEVICE_NOT_FOUND when the device is already unpaired (AAA)")
  void handleUnpairAlreadyUnpaired() {
    // Arrange
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.unpairedDevice()));

    // Act
    var result = service.handle(DeviceCommandFixtures.unpairDeviceCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("DEVICE_NOT_FOUND");
    verifyNoInteractions(externalAssetManagementService);
  }

  @Test
  @DisplayName("handle(UnpairDeviceCommand) -> DEVICE_NOT_FOUND when the device belongs to another owner's equipment (AAA)")
  void handleUnpairForeignDevice() {
    // Arrange
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.pairedDevice()));
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.unpairDeviceCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("DEVICE_NOT_FOUND");
    verify(deviceRepository, never()).save(any(Device.class));
  }

  // ====================================================================================
  // handle(RotateApiKeyCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(RotateApiKeyCommand) -> replaces the digest and returns the new plain API key (AAA)")
  void handleRotatePairedDevice() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();
    stubReachableDevice(device);
    stubIssuedCredential(OTHER_DIGEST);
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.rotateApiKeyCommand());

    // Assert
    var credential = ResultTestUtils.successValue(result);
    assertThat(credential.apiKey()).isEqualTo(VALID_API_KEY);
    assertThat(credential.device().getApiKeyDigest()).isEqualTo(OTHER_DIGEST);
    assertThat(credential.device().getStatus()).isEqualTo(DeviceStatus.PAIRED);
    verify(deviceRepository, times(1)).save(device);
  }

  @Test
  @DisplayName("handle(RotateApiKeyCommand) -> resumes a revoked device with the new credential and raises a status change (AAA)")
  void handleRotateRevokedDevice() {
    // Arrange
    stubReachableDevice(DeviceDomainFixtures.revokedDevice());
    stubIssuedCredential(OTHER_DIGEST);
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.rotateApiKeyCommand());

    // Assert
    var device = ResultTestUtils.successValue(result).device();
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    assertThat(device.domainEvents()).hasAtLeastOneElementOfType(DeviceStatusChangedEvent.class);
  }

  @Test
  @DisplayName("handle(RotateApiKeyCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleRotateNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(DeviceCommandFixtures.rotateApiKeyCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(deviceRepository, deviceCredentialService);
  }

  @Test
  @DisplayName("handle(RotateApiKeyCommand) -> DEVICE_NOT_FOUND for an unpaired device, issuing no credential (AAA)")
  void handleRotateUnpairedDevice() {
    // Arrange
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.unpairedDevice()));

    // Act
    var result = service.handle(DeviceCommandFixtures.rotateApiKeyCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("DEVICE_NOT_FOUND");
    verifyNoInteractions(deviceCredentialService);
  }

  @Test
  @DisplayName("handle(RotateApiKeyCommand) -> DEVICE_NOT_FOUND for another owner's device, issuing no credential (AAA)")
  void handleRotateForeignDevice() {
    // Arrange
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.pairedDevice()));
    when(externalAssetManagementService.existsEquipmentForOwner(EQUIPMENT_ID, OWNER_ID)).thenReturn(false);

    // Act
    var result = service.handle(DeviceCommandFixtures.rotateApiKeyCommand());

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("DEVICE_NOT_FOUND");
    verifyNoInteractions(deviceCredentialService);
    verify(deviceRepository, never()).save(any(Device.class));
  }

  // ====================================================================================
  // handle(ChangeDeviceStatusCommand)
  // ====================================================================================

  @Test
  @DisplayName("handle(ChangeDeviceStatusCommand) -> REVOKED revokes the credential but keeps the equipment (AAA)")
  void handleChangeStatusToRevoked() {
    // Arrange
    stubReachableDevice(DeviceDomainFixtures.pairedDevice());
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.changeDeviceStatusCommand(DeviceStatus.REVOKED));

    // Assert
    var device = ResultTestUtils.successValue(result);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
    assertThat(device.getApiKeyDigest()).isNull();
    assertThat(device.getPairedEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(device.domainEvents()).hasAtLeastOneElementOfType(DeviceStatusChangedEvent.class);
  }

  @Test
  @DisplayName("handle(ChangeDeviceStatusCommand) -> UNPAIRED unpairs the device (AAA)")
  void handleChangeStatusToUnpaired() {
    // Arrange
    stubReachableDevice(DeviceDomainFixtures.pairedDevice());
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.changeDeviceStatusCommand(DeviceStatus.UNPAIRED));

    // Assert
    var device = ResultTestUtils.successValue(result);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
    assertThat(device.getPairedEquipmentId()).isNull();
  }

  @Test
  @DisplayName("handle(ChangeDeviceStatusCommand) -> PAIRED on an already paired device changes nothing (AAA)")
  void handleChangeStatusToPairedWhenPaired() {
    // Arrange
    stubReachableDevice(DeviceDomainFixtures.pairedDevice());
    stubSaveReturnsArgument();

    // Act
    var result = service.handle(DeviceCommandFixtures.changeDeviceStatusCommand(DeviceStatus.PAIRED));

    // Assert
    var device = ResultTestUtils.successValue(result);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    assertThat(device.getApiKeyDigest()).isEqualTo(VALID_DIGEST);
  }

  @Test
  @DisplayName("handle(ChangeDeviceStatusCommand) -> DEVICE_CONFLICT when a revoked device is asked to resume as PAIRED (AAA)")
  void handleChangeStatusRevokedToPaired() {
    // Arrange
    stubReachableDevice(DeviceDomainFixtures.revokedDevice());

    // Act
    var result = service.handle(DeviceCommandFixtures.changeDeviceStatusCommand(DeviceStatus.PAIRED));

    // Assert
    var error = ResultTestUtils.failureError(result);
    assertThat(error.code()).isEqualTo("DEVICE_CONFLICT");
    assertThat(error.details()).contains("rotate its API key");
    verify(deviceRepository, never()).save(any(Device.class));
  }

  @Test
  @DisplayName("handle(ChangeDeviceStatusCommand) -> ACCESS_DENIED when the caller holds no owner profile (AAA)")
  void handleChangeStatusNoOwnerProfile() {
    // Arrange
    stubNoOwnerProfile();

    // Act
    var result = service.handle(DeviceCommandFixtures.changeDeviceStatusCommand(DeviceStatus.REVOKED));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("ACCESS_DENIED");
    verifyNoInteractions(deviceRepository);
  }

  @Test
  @DisplayName("handle(ChangeDeviceStatusCommand) -> DEVICE_NOT_FOUND for a device the caller cannot reach (AAA)")
  void handleChangeStatusMissingDevice() {
    // Arrange
    stubOwner();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

    // Act
    var result = service.handle(DeviceCommandFixtures.changeDeviceStatusCommand(DeviceStatus.REVOKED));

    // Assert
    assertThat(ResultTestUtils.failureError(result).code()).isEqualTo("DEVICE_NOT_FOUND");
    verify(deviceRepository, never()).save(any(Device.class));
  }

  // ====================================================================================
  // recordActivity(Long, Instant)
  // ====================================================================================

  @Test
  @DisplayName("recordActivity(Long, Instant) -> stamps lastSeenAt on a paired device and saves it (AAA)")
  void recordActivityPairedDevice() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));

    // Act
    service.recordActivity(DEVICE_ID, LAST_SEEN_AT);

    // Assert
    assertThat(device.getLastSeenAt()).isEqualTo(LAST_SEEN_AT);
    verify(deviceRepository, times(1)).save(device);
  }

  @Test
  @DisplayName("recordActivity(Long, Instant) -> ignores a revoked device (AAA)")
  void recordActivityRevokedDevice() {
    // Arrange
    var device = DeviceDomainFixtures.revokedDevice();
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(device));

    // Act
    service.recordActivity(DEVICE_ID, LAST_SEEN_AT);

    // Assert
    assertThat(device.getLastSeenAt()).isNull();
    verify(deviceRepository, never()).save(any(Device.class));
  }

  @Test
  @DisplayName("recordActivity(Long, Instant) -> ignores an unpaired device (AAA)")
  void recordActivityUnpairedDevice() {
    // Arrange
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.of(DeviceDomainFixtures.unpairedDevice()));

    // Act
    service.recordActivity(DEVICE_ID, LAST_SEEN_AT);

    // Assert
    verify(deviceRepository, never()).save(any(Device.class));
  }

  @Test
  @DisplayName("recordActivity(Long, Instant) -> ignores an unknown device (AAA)")
  void recordActivityMissingDevice() {
    // Arrange
    when(deviceRepository.findById(DEVICE_ID)).thenReturn(Optional.empty());

    // Act
    service.recordActivity(DEVICE_ID, Instant.now());

    // Assert
    verify(deviceRepository, times(1)).findById(DEVICE_ID);
    verifyNoMoreInteractions(deviceRepository);
  }
}
