package pe.edu.upc.ice.track.platform.device.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DevicePairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceRegisteredEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceStatusChangedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.events.DeviceUnpairedEvent;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceStatus;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

class DeviceTest {

  @Test
  @DisplayName("Device(Long, String, String, String) -> starts UNPAIRED with no equipment, credential or activity (AAA)")
  void constructorStartsUnpaired() {
    // Act
    var device = new Device(null, "  " + VALID_DEVICE_UID + "  ", VALID_BOARD_MODEL, VALID_FIRMWARE_VERSION);

    // Assert
    assertThat(device.getDeviceUid()).isEqualTo(VALID_DEVICE_UID);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
    assertThat(device.getPairedEquipmentId()).isNull();
    assertThat(device.getApiKeyDigest()).isNull();
    assertThat(device.getLastSeenAt()).isNull();
    assertThat(device.isPaired()).isFalse();
    assertThat(device.isLinkedToEquipment()).isFalse();
  }

  @ParameterizedTest(name = "[{index}] deviceUid={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("Device(...) -> rejects a blank device uid (AAA)")
  void constructorRejectsBlankUid(String uid) {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new Device(null, uid, VALID_BOARD_MODEL, VALID_FIRMWARE_VERSION));
  }

  @ParameterizedTest(name = "[{index}] boardModel={0}")
  @MethodSource("pe.edu.upc.ice.track.platform.shared.test.fixtures.CommonCommandFixtures#invalidBlankStrings")
  @DisplayName("Device(...) -> rejects a blank board model (AAA)")
  void constructorRejectsBlankBoardModel(String boardModel) {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new Device(null, VALID_DEVICE_UID, boardModel, VALID_FIRMWARE_VERSION));
  }

  @Test
  @DisplayName("Device(...) -> rejects a uid, board model or firmware version longer than 64 characters (AAA)")
  void constructorRejectsTooLongValues() {
    // Arrange
    var tooLong = "x".repeat(65);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new Device(null, tooLong, VALID_BOARD_MODEL, null));
    assertThrows(IllegalArgumentException.class, () -> new Device(null, VALID_DEVICE_UID, tooLong, null));
    assertThrows(IllegalArgumentException.class, () -> new Device(null, VALID_DEVICE_UID, VALID_BOARD_MODEL, tooLong));
  }

  @Test
  @DisplayName("Device(...) -> a blank firmware version is stored as null (AAA)")
  void constructorBlankFirmwareBecomesNull() {
    // Act
    var device = new Device(null, VALID_DEVICE_UID, VALID_BOARD_MODEL, "   ");

    // Assert
    assertThat(device.getFirmwareVersion()).isNull();
  }

  @Test
  @DisplayName("Device(...) -> rejects inconsistent reconstitution states (AAA)")
  void constructorRejectsInconsistentState() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> DeviceDomainFixtures.deviceBuilder().equipmentId(EQUIPMENT_ID).build());
    assertThrows(IllegalArgumentException.class, () -> new Device(
        1L, VALID_DEVICE_UID, VALID_BOARD_MODEL, null, null, DeviceStatus.PAIRED, EQUIPMENT_ID, null));
    assertThrows(IllegalArgumentException.class, () -> new Device(
        1L, VALID_DEVICE_UID, VALID_BOARD_MODEL, null, VALID_DIGEST, DeviceStatus.REVOKED, EQUIPMENT_ID, null));
    assertThrows(IllegalArgumentException.class, () -> new Device(
        1L, VALID_DEVICE_UID, VALID_BOARD_MODEL, null, VALID_DIGEST, DeviceStatus.UNPAIRED, null, null));
  }

  @Test
  @DisplayName("Device(...) -> rejects a digest that is not 64 lowercase hex characters (AAA)")
  void constructorRejectsMalformedDigest() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new Device(
        1L, VALID_DEVICE_UID, VALID_BOARD_MODEL, null, "A".repeat(64), DeviceStatus.PAIRED, EQUIPMENT_ID, null));
    assertThrows(IllegalArgumentException.class, () -> new Device(
        1L, VALID_DEVICE_UID, VALID_BOARD_MODEL, null, "abc", DeviceStatus.PAIRED, EQUIPMENT_ID, null));
  }

  @Test
  @DisplayName("pairTo(Long, String) -> becomes PAIRED and registers paired and status-changed events (AAA)")
  void pairToSuccess() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act
    device.pairTo(EQUIPMENT_ID, VALID_DIGEST);

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    assertThat(device.getPairedEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(device.getApiKeyDigest()).isEqualTo(VALID_DIGEST);
    assertThat(device.isPaired()).isTrue();
    assertThat(device.domainEvents()).hasSize(2)
        .hasAtLeastOneElementOfType(DevicePairedEvent.class)
        .hasAtLeastOneElementOfType(DeviceStatusChangedEvent.class);
  }

  @Test
  @DisplayName("pairTo(Long, String) -> resets lastSeenAt (AAA)")
  void pairToResetsLastSeen() {
    // Arrange
    var device = DeviceDomainFixtures.deviceBuilder().lastSeenAt(null).build();

    // Act
    device.pairTo(EQUIPMENT_ID, VALID_DIGEST);

    // Assert
    assertThat(device.getLastSeenAt()).isNull();
  }

  @Test
  @DisplayName("pairTo(Long, String) -> skips the status-changed event for a device that was never persisted (AAA)")
  void pairToUnsavedDeviceRaisesNoStatusEvent() {
    // Arrange
    var device = DeviceDomainFixtures.deviceBuilder().id(null).build();

    // Act
    device.pairTo(EQUIPMENT_ID, VALID_DIGEST);

    // Assert
    assertThat(device.domainEvents()).hasSize(1).hasOnlyElementsOfType(DevicePairedEvent.class);
  }

  @Test
  @DisplayName("pairTo(Long, String) -> IllegalStateException when the device is already paired (AAA)")
  void pairToAlreadyPaired() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> device.pairTo(EQUIPMENT_ID, OTHER_DIGEST));
  }

  @Test
  @DisplayName("pairTo(Long, String) -> rejects null and non positive equipment ids (AAA)")
  void pairToInvalidEquipmentId() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> device.pairTo(null, VALID_DIGEST));
    assertThrows(IllegalArgumentException.class, () -> device.pairTo(0L, VALID_DIGEST));
    assertThrows(IllegalArgumentException.class, () -> device.pairTo(-5L, VALID_DIGEST));
  }

  @Test
  @DisplayName("pairTo(Long, String) -> rejects a malformed digest, staying UNPAIRED (AAA)")
  void pairToMalformedDigest() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> device.pairTo(EQUIPMENT_ID, "short"));
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
  }

  @Test
  @DisplayName("rotateCredential(String) -> replaces the digest of a paired device (AAA)")
  void rotateCredentialPaired() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.rotateCredential(OTHER_DIGEST);

    // Assert
    assertThat(device.getApiKeyDigest()).isEqualTo(OTHER_DIGEST);
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
  }

  @Test
  @DisplayName("rotateCredential(String) -> a revoked device becomes PAIRED again (AAA)")
  void rotateCredentialRevoked() {
    // Arrange
    var device = DeviceDomainFixtures.revokedDevice();

    // Act
    device.rotateCredential(OTHER_DIGEST);

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    assertThat(device.isPaired()).isTrue();
  }

  @Test
  @DisplayName("rotateCredential(String) -> IllegalStateException for an unpaired device (AAA)")
  void rotateCredentialUnpaired() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act & Assert
    assertThrows(IllegalStateException.class, () -> device.rotateCredential(OTHER_DIGEST));
  }

  @Test
  @DisplayName("revokeCredential() -> drops the digest, keeps the equipment and raises one status change (AAA)")
  void revokeCredentialPaired() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.revokeCredential();

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
    assertThat(device.getApiKeyDigest()).isNull();
    assertThat(device.getPairedEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(device.isLinkedToEquipment()).isTrue();
    assertThat(device.isPaired()).isFalse();
    assertThat(device.domainEvents()).hasSize(1).hasOnlyElementsOfType(DeviceStatusChangedEvent.class);
  }

  @Test
  @DisplayName("revokeCredential() -> revoking an already revoked device raises no further event (AAA)")
  void revokeCredentialIdempotent() {
    // Arrange
    var device = DeviceDomainFixtures.revokedDevice();

    // Act
    device.revokeCredential();

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
    assertThat(device.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("revokeCredential() -> IllegalStateException for an unpaired device (AAA)")
  void revokeCredentialUnpaired() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act & Assert
    assertThrows(IllegalStateException.class, device::revokeCredential);
  }

  @Test
  @DisplayName("unpair() -> clears equipment and credential and raises unpaired and status-changed events (AAA)")
  void unpairPaired() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.unpair();

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
    assertThat(device.getPairedEquipmentId()).isNull();
    assertThat(device.getApiKeyDigest()).isNull();
    assertThat(device.domainEvents()).hasSize(2)
        .hasAtLeastOneElementOfType(DeviceUnpairedEvent.class)
        .hasAtLeastOneElementOfType(DeviceStatusChangedEvent.class);
  }

  @Test
  @DisplayName("unpair() -> the unpaired event carries the equipment the device was linked to (AAA)")
  void unpairEventCarriesPreviousEquipment() {
    // Arrange
    var device = DeviceDomainFixtures.revokedDevice();

    // Act
    device.unpair();

    // Assert
    var event = device.domainEvents().stream()
        .filter(DeviceUnpairedEvent.class::isInstance).map(DeviceUnpairedEvent.class::cast).findFirst();
    assertThat(event).isPresent();
    assertThat(event.get().equipmentId()).isEqualTo(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("unpair() -> IllegalStateException for a device that is not paired (AAA)")
  void unpairUnpaired() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act & Assert
    assertThrows(IllegalStateException.class, device::unpair);
  }

  @Test
  @DisplayName("changeStatus(DeviceStatus) -> REVOKED revokes the credential (AAA)")
  void changeStatusRevoked() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.changeStatus(DeviceStatus.REVOKED);

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.REVOKED);
  }

  @Test
  @DisplayName("changeStatus(DeviceStatus) -> UNPAIRED unpairs the device (AAA)")
  void changeStatusUnpaired() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.changeStatus(DeviceStatus.UNPAIRED);

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.UNPAIRED);
  }

  @Test
  @DisplayName("changeStatus(DeviceStatus) -> PAIRED on a paired device is a no-op (AAA)")
  void changeStatusPairedNoop() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.changeStatus(DeviceStatus.PAIRED);

    // Assert
    assertThat(device.getStatus()).isEqualTo(DeviceStatus.PAIRED);
    assertThat(device.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("changeStatus(DeviceStatus) -> PAIRED on a revoked device is refused: it needs a new credential (AAA)")
  void changeStatusRevokedToPaired() {
    // Arrange
    var device = DeviceDomainFixtures.revokedDevice();

    // Act & Assert
    var error = assertThrows(IllegalStateException.class, () -> device.changeStatus(DeviceStatus.PAIRED));
    assertThat(error.getMessage()).contains("rotate its API key");
  }

  @Test
  @DisplayName("changeStatus(DeviceStatus) -> NullPointerException for a null target (AAA)")
  void changeStatusNull() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act & Assert
    assertThrows(NullPointerException.class, () -> device.changeStatus(null));
  }

  @Test
  @DisplayName("describeHardware(String, String) -> updates board model and firmware version (AAA)")
  void describeHardware() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act
    device.describeHardware(" ESP32-C6 ", "2.0.0");

    // Assert
    assertThat(device.getBoardModel()).isEqualTo("ESP32-C6");
    assertThat(device.getFirmwareVersion()).isEqualTo("2.0.0");
  }

  @Test
  @DisplayName("describeHardware(String, String) -> rejects a blank board model and keeps the previous one (AAA)")
  void describeHardwareBlankBoardModel() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> device.describeHardware(" ", "2.0.0"));
    assertThat(device.getBoardModel()).isEqualTo(VALID_BOARD_MODEL);
  }

  @Test
  @DisplayName("recordActivity(Instant) -> stamps lastSeenAt of a paired device (AAA)")
  void recordActivityPaired() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act
    device.recordActivity(LAST_SEEN_AT);

    // Assert
    assertThat(device.getLastSeenAt()).isEqualTo(LAST_SEEN_AT);
  }

  @Test
  @DisplayName("recordActivity(Instant) -> IllegalStateException unless the device is paired (AAA)")
  void recordActivityNotPaired() {
    // Act & Assert
    assertThrows(IllegalStateException.class, () -> DeviceDomainFixtures.unpairedDevice().recordActivity(Instant.now()));
    assertThrows(IllegalStateException.class, () -> DeviceDomainFixtures.revokedDevice().recordActivity(Instant.now()));
  }

  @Test
  @DisplayName("recordActivity(Instant) -> NullPointerException for a null instant (AAA)")
  void recordActivityNull() {
    // Arrange
    var device = DeviceDomainFixtures.pairedDevice();

    // Act & Assert
    assertThrows(NullPointerException.class, () -> device.recordActivity(null));
  }

  @Test
  @DisplayName("onRegistered() -> registers a DeviceRegisteredEvent for the uid (AAA)")
  void onRegistered() {
    // Arrange
    var device = DeviceDomainFixtures.unpairedDevice();

    // Act
    device.onRegistered();

    // Assert
    assertThat(device.domainEvents()).hasSize(1).hasOnlyElementsOfType(DeviceRegisteredEvent.class);
    var event = (DeviceRegisteredEvent) device.domainEvents().iterator().next();
    assertThat(event.deviceUid()).isEqualTo(VALID_DEVICE_UID);
  }
}
