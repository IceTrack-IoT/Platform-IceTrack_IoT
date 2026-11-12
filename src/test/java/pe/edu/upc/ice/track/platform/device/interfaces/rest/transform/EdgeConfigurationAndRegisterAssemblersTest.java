package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.device.application.results.EdgeConfiguration;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.RegisterDeviceResource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

/** Covers {@code EdgeConfigurationResourceFromResultAssembler} and {@code RegisterDeviceCommandFromResourceAssembler}. */
class EdgeConfigurationAndRegisterAssemblersTest {

  @Test
  @DisplayName("EdgeConfigurationResourceFromResultAssembler.toResourceFromResult -> maps values and renders grant expiry as ISO-8601 (AAA)")
  void edgeConfigurationToResource() {
    // Arrange
    var expiresAt = Instant.parse("2026-02-01T00:00:00Z");
    var configuration = new EdgeConfiguration(
        4, 2.0, 8.0, 15, 0.5, 3, List.of(new EdgeConfiguration.Grant("sha-1", 5L, 9L, expiresAt)));

    // Act
    var resource = EdgeConfigurationResourceFromResultAssembler.toResourceFromResult(configuration);

    // Assert
    assertThat(resource.configVersion()).isEqualTo(4);
    assertThat(resource.minCelsius()).isEqualTo(2.0);
    assertThat(resource.maxCelsius()).isEqualTo(8.0);
    assertThat(resource.sustainedExcursionMinutes()).isEqualTo(15);
    assertThat(resource.hysteresisMarginCelsius()).isEqualTo(0.5);
    assertThat(resource.missedSyncWindowsForOffline()).isEqualTo(3);
    assertThat(resource.grants()).hasSize(1);
    var grant = resource.grants().get(0);
    assertThat(grant.sha256()).isEqualTo("sha-1");
    assertThat(grant.technicianId()).isEqualTo(5L);
    assertThat(grant.serviceRequestId()).isEqualTo(9L);
    assertThat(grant.expiresAt()).isEqualTo("2026-02-01T00:00:00Z");
  }

  @Test
  @DisplayName("EdgeConfigurationResourceFromResultAssembler.toResourceFromResult -> empty grants stay empty (AAA)")
  void edgeConfigurationNoGrants() {
    // Arrange
    var configuration = new EdgeConfiguration(1, 2.0, 8.0, 10, 0.3, 2, List.of());

    // Act
    var resource = EdgeConfigurationResourceFromResultAssembler.toResourceFromResult(configuration);

    // Assert
    assertThat(resource.grants()).isEmpty();
  }

  @Test
  @DisplayName("RegisterDeviceCommandFromResourceAssembler.toCommandFromResource -> maps the resource and the caller (AAA)")
  void registerDeviceToCommand() {
    // Arrange
    var resource = new RegisterDeviceResource(EQUIPMENT_ID, VALID_DEVICE_UID, VALID_BOARD_MODEL, VALID_FIRMWARE_VERSION);

    // Act
    var command = RegisterDeviceCommandFromResourceAssembler.toCommandFromResource(USERNAME, resource);

    // Assert
    assertThat(command.username()).isEqualTo(USERNAME);
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.deviceUid()).isEqualTo(VALID_DEVICE_UID);
    assertThat(command.boardModel()).isEqualTo(VALID_BOARD_MODEL);
    assertThat(command.firmwareVersion()).isEqualTo(VALID_FIRMWARE_VERSION);
  }

  @Test
  @DisplayName("RegisterDeviceCommandFromResourceAssembler.toCommandFromResource -> keeps a missing firmware version null (AAA)")
  void registerDeviceNullFirmware() {
    // Arrange
    var resource = new RegisterDeviceResource(EQUIPMENT_ID, VALID_DEVICE_UID, VALID_BOARD_MODEL, null);

    // Act
    var command = RegisterDeviceCommandFromResourceAssembler.toCommandFromResource(USERNAME, resource);

    // Assert
    assertThat(command.firmwareVersion()).isNull();
  }
}
