package pe.edu.upc.ice.track.platform.device.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceCredentialResult;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetails;
import pe.edu.upc.ice.track.platform.device.application.results.DeviceDetailsPage;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivity;
import pe.edu.upc.ice.track.platform.device.domain.model.valueobjects.DeviceConnectivityState;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceConnectivityStateResource;
import pe.edu.upc.ice.track.platform.device.interfaces.rest.resources.DeviceStatusResource;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures.*;

/** Covers {@code DeviceResourceFromEntityAssembler} and {@code DeviceCredentialResourceAssembler}. */
class DeviceResourceAssemblersTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:10:00Z");

  private static DeviceConnectivity reporting() {
    return new DeviceConnectivity(DeviceConnectivityState.REPORTING, Duration.ofMinutes(3), null, NOW);
  }

  private static DeviceDetails pairedDetails() {
    var device = DeviceDomainFixtures.deviceBuilder().paired().lastSeenAt(LAST_SEEN_AT).build();
    return new DeviceDetails(device, "Refrigerador 1", SITE_ID, reporting());
  }


  @Test
  @DisplayName("toResourceFromDetails(DeviceDetails) -> maps the device, its equipment and connectivity (AAA)")
  void toResourceFromDetails() {
    // Act
    var resource = DeviceResourceFromEntityAssembler.toResourceFromDetails(pairedDetails());

    // Assert
    assertThat(resource.id()).isEqualTo(DEVICE_ID);
    assertThat(resource.deviceUid()).isEqualTo(VALID_DEVICE_UID);
    assertThat(resource.boardModel()).isEqualTo(VALID_BOARD_MODEL);
    assertThat(resource.firmwareVersion()).isEqualTo(VALID_FIRMWARE_VERSION);
    assertThat(resource.status()).isEqualTo(DeviceStatusResource.PAIRED);
    assertThat(resource.pairedEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(resource.pairedEquipmentName()).isEqualTo("Refrigerador 1");
    assertThat(resource.pairedSiteId()).isEqualTo(SITE_ID);
    assertThat(resource.lastSeenAt()).isEqualTo(LAST_SEEN_AT);
    assertThat(resource.connectivity().state()).isEqualTo(DeviceConnectivityStateResource.REPORTING);
  }

  @Test
  @DisplayName("toResourceFromDetails(DeviceDetails) -> never exposes the API key digest (AAA)")
  void toResourceFromDetailsHidesDigest() {
    // Act
    var resource = DeviceResourceFromEntityAssembler.toResourceFromDetails(pairedDetails());

    // Assert
    assertThat(resource.toString()).doesNotContain(DeviceDomainFixtures.VALID_DIGEST);
  }

  @Test
  @DisplayName("toResourceFromDetails(DeviceDetails) -> leaves equipment fields null for an unpaired device (AAA)")
  void toResourceFromDetailsUnpaired() {
    // Arrange
    var connectivity = new DeviceConnectivity(DeviceConnectivityState.UNPAIRED, null, null, NOW);
    var details = DeviceDetails.withoutEquipment(DeviceDomainFixtures.unpairedDevice(), connectivity);

    // Act
    var resource = DeviceResourceFromEntityAssembler.toResourceFromDetails(details);

    // Assert
    assertThat(resource.status()).isEqualTo(DeviceStatusResource.UNPAIRED);
    assertThat(resource.pairedEquipmentId()).isNull();
    assertThat(resource.pairedEquipmentName()).isNull();
    assertThat(resource.pairedSiteId()).isNull();
    assertThat(resource.lastSeenAt()).isNull();
    assertThat(resource.connectivity().state()).isEqualTo(DeviceConnectivityStateResource.UNPAIRED);
  }


  @Test
  @DisplayName("toResourceFromConnectivity(DeviceConnectivity) -> null for null (AAA)")
  void toResourceFromConnectivityNull() {
    // Act & Assert
    assertThat(DeviceResourceFromEntityAssembler.toResourceFromConnectivity(null)).isNull();
  }

  @Test
  @DisplayName("toResourceFromConnectivity(DeviceConnectivity) -> converts the window to whole seconds (AAA)")
  void toResourceFromConnectivityWindow() {
    // Arrange
    var silentSince = LAST_SEEN_AT.plusSeconds(180);
    var connectivity = new DeviceConnectivity(DeviceConnectivityState.SILENT, Duration.ofMinutes(3), silentSince, NOW);

    // Act
    var resource = DeviceResourceFromEntityAssembler.toResourceFromConnectivity(connectivity);

    // Assert
    assertThat(resource.state()).isEqualTo(DeviceConnectivityStateResource.SILENT);
    assertThat(resource.silentAfterSeconds()).isEqualTo(180);
    assertThat(resource.silentSince()).isEqualTo(silentSince);
    assertThat(resource.evaluatedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("toResourceFromConnectivity(DeviceConnectivity) -> silentAfterSeconds is null when no window is known (AAA)")
  void toResourceFromConnectivityNoWindow() {
    // Arrange
    var connectivity = new DeviceConnectivity(DeviceConnectivityState.UNPAIRED, null, null, NOW);

    // Act
    var resource = DeviceResourceFromEntityAssembler.toResourceFromConnectivity(connectivity);

    // Assert
    assertThat(resource.silentAfterSeconds()).isNull();
  }


  @Test
  @DisplayName("toResourcesFromDetails(List) -> maps every item preserving order (AAA)")
  void toResourcesFromDetails() {
    // Arrange
    var second = new DeviceDetails(
        DeviceDomainFixtures.deviceBuilder().id(OTHER_DEVICE_ID).uid("ESP32-DEV-0002").paired().build(),
        "Cámara 2", SITE_ID, reporting());

    // Act
    var resources = DeviceResourceFromEntityAssembler.toResourcesFromDetails(List.of(pairedDetails(), second));

    // Assert
    assertThat(resources).extracting(r -> r.id()).containsExactly(DEVICE_ID, OTHER_DEVICE_ID);
  }

  @Test
  @DisplayName("toPagedResourceFromPage(DeviceDetailsPage) -> maps the content and the page metadata (AAA)")
  void toPagedResourceFromPage() {
    // Arrange
    var page = new DeviceDetailsPage(List.of(pairedDetails()), 1, 10, 25);

    // Act
    var resource = DeviceResourceFromEntityAssembler.toPagedResourceFromPage(page);

    // Assert
    assertThat(resource.content()).hasSize(1);
    assertThat(resource.page()).isEqualTo(1);
    assertThat(resource.size()).isEqualTo(10);
    assertThat(resource.totalElements()).isEqualTo(25);
    assertThat(resource.totalPages()).isEqualTo(3);
  }


  @Test
  @DisplayName("toResourceFromResult(DeviceCredentialResult, DeviceDetails) -> pairs the device resource with the plain API key (AAA)")
  void toCredentialResource() {
    // Arrange
    var details = pairedDetails();
    var result = new DeviceCredentialResult(details.device(), VALID_API_KEY);

    // Act
    var resource = DeviceCredentialResourceAssembler.toResourceFromResult(result, details);

    // Assert
    assertThat(resource.apiKey()).isEqualTo(VALID_API_KEY);
    assertThat(resource.device().id()).isEqualTo(DEVICE_ID);
    assertThat(resource.device().status()).isEqualTo(DeviceStatusResource.PAIRED);
  }
}
