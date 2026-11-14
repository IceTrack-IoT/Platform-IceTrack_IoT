package pe.edu.upc.ice.track.platform.assets.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.ConnectivityStateResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.EquipmentTypeResource;
import pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources.StatusEquipmentResource;
import pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.EQUIPMENT_ID;
import static pe.edu.upc.ice.track.platform.assets.test.fixtures.AssetsDomainFixtures.OTHER_EQUIPMENT_ID;

class EquipmentResourceFromEntityAssemblerTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:10:00Z");
  private static final Instant LAST_RECEIVED = Instant.parse("2026-01-01T10:09:30Z");

  private EquipmentConnectivity connectivity() {
    return new EquipmentConnectivity(
        ConnectivityState.REPORTING, LAST_RECEIVED, Duration.ofMinutes(3), null, false, NOW);
  }

  @Test
  @DisplayName("toResourceFromEntity(Equipment, EquipmentConnectivity) -> maps every attribute of the unit (AAA)")
  void toResourceMapsUnit() {
    // Arrange
    var readingAt = Instant.parse("2026-01-01T10:09:00Z");
    var unit = AssetsDomainFixtures.equipmentBuilder()
        .status(StatusEquipment.MAINTENANCE).online(true).type(EquipmentType.COLD_ROOM)
        .lastReading(readingAt, 4.5).configVersion(3).version(7L).build();
    unit.attachSiteName("Sede Central");

    // Act
    var resource = EquipmentResourceFromEntityAssembler.toResourceFromEntity(unit, connectivity());

    // Assert
    assertThat(resource.id()).isEqualTo(EQUIPMENT_ID);
    assertThat(resource.siteId()).isEqualTo(AssetsDomainFixtures.SITE_ID);
    assertThat(resource.siteName()).isEqualTo("Sede Central");
    assertThat(resource.uid()).isEqualTo(AssetsDomainFixtures.VALID_UID);
    assertThat(resource.name()).isEqualTo(AssetsDomainFixtures.VALID_EQUIPMENT_NAME);
    assertThat(resource.equipmentType()).isEqualTo(EquipmentTypeResource.COLD_ROOM);
    assertThat(resource.status()).isEqualTo(StatusEquipmentResource.MAINTENANCE);
    assertThat(resource.online()).isTrue();
    assertThat(resource.minCelsius()).isEqualTo(AssetsDomainFixtures.VALID_MIN_CELSIUS);
    assertThat(resource.maxCelsius()).isEqualTo(AssetsDomainFixtures.VALID_MAX_CELSIUS);
    assertThat(resource.reminderIntervalDays()).isEqualTo(AssetsDomainFixtures.VALID_REMINDER_INTERVAL_DAYS);
    assertThat(resource.lastReadingAt()).isEqualTo(readingAt);
    assertThat(resource.lastKnownTemperature()).isEqualTo(4.5);
    assertThat(resource.configVersion()).isEqualTo(3);
    assertThat(resource.version()).isEqualTo(7L);
  }

  @Test
  @DisplayName("toResourceFromEntity(Equipment, EquipmentConnectivity) -> maps the connectivity, converting the window to seconds (AAA)")
  void toResourceMapsConnectivity() {
    // Act
    var resource = EquipmentResourceFromEntityAssembler.toResourceFromEntity(
        AssetsDomainFixtures.equipment(), connectivity());

    // Assert
    var mapped = resource.connectivity();
    assertThat(mapped.state()).isEqualTo(ConnectivityStateResource.REPORTING);
    assertThat(mapped.lastReceivedAt()).isEqualTo(LAST_RECEIVED);
    assertThat(mapped.silentAfterSeconds()).isEqualTo(180L);
    assertThat(mapped.silentSince()).isNull();
    assertThat(mapped.underMaintenance()).isFalse();
    assertThat(mapped.evaluatedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("toResourceFromEntity(Equipment, EquipmentConnectivity) -> leaves connectivity null when none was evaluated (AAA)")
  void toResourceNullConnectivity() {
    // Act
    var resource = EquipmentResourceFromEntityAssembler.toResourceFromEntity(AssetsDomainFixtures.equipment(), null);

    // Assert
    assertThat(resource.connectivity()).isNull();
  }

  @Test
  @DisplayName("toResourceFromEntity(Equipment, EquipmentConnectivity) -> reports version 0 for a unit with a null version (AAA)")
  void toResourceNullVersion() {
    // Arrange
    var unit = AssetsDomainFixtures.equipmentBuilder().version(null).build();

    // Act
    var resource = EquipmentResourceFromEntityAssembler.toResourceFromEntity(unit, null);

    // Assert
    assertThat(resource.version()).isZero();
  }

  @Test
  @DisplayName("toPagedResourceFromPage(EquipmentPage, Map) -> maps the content with its own connectivity and the page metadata (AAA)")
  void toPagedResourceMapsPage() {
    // Arrange
    var first = AssetsDomainFixtures.equipment();
    var second = AssetsDomainFixtures.equipmentBuilder().id(OTHER_EQUIPMENT_ID).uid("ESP32-0002").build();
    var page = new EquipmentPage(List.of(first, second), 1, 2, 5);

    // Act
    var resource = EquipmentResourceFromEntityAssembler.toPagedResourceFromPage(
        page, Map.of(EQUIPMENT_ID, connectivity()));

    // Assert
    assertThat(resource.content()).hasSize(2);
    assertThat(resource.content().get(0).id()).isEqualTo(EQUIPMENT_ID);
    assertThat(resource.content().get(0).connectivity()).isNotNull();
    assertThat(resource.content().get(1).id()).isEqualTo(OTHER_EQUIPMENT_ID);
    assertThat(resource.content().get(1).connectivity()).isNull();
    assertThat(resource.page()).isEqualTo(1);
    assertThat(resource.size()).isEqualTo(2);
    assertThat(resource.totalElements()).isEqualTo(5);
    assertThat(resource.totalPages()).isEqualTo(3);
  }

  @Test
  @DisplayName("toPagedResourceFromPage(EquipmentPage, Map) -> maps an empty page to an empty content with one total page (AAA)")
  void toPagedResourceEmptyPage() {
    // Act
    var resource = EquipmentResourceFromEntityAssembler.toPagedResourceFromPage(
        EquipmentPage.unpaged(List.of()), Map.of());

    // Assert
    assertThat(resource.content()).isEmpty();
    assertThat(resource.totalElements()).isZero();
    assertThat(resource.totalPages()).isEqualTo(1);
  }

  @Test
  @DisplayName("toResourceFromConnectivity(EquipmentConnectivity) -> returns null for null (AAA)")
  void toResourceFromConnectivityNull() {
    // Act & Assert
    assertThat(EquipmentResourceFromEntityAssembler.toResourceFromConnectivity(null)).isNull();
  }

  @Test
  @DisplayName("toResourceFromConnectivity(EquipmentConnectivity) -> maps a silent unit with its silentSince (AAA)")
  void toResourceFromConnectivitySilent() {
    // Arrange
    var silentSince = LAST_RECEIVED.plusSeconds(180);
    var silent = new EquipmentConnectivity(
        ConnectivityState.SILENT, LAST_RECEIVED, Duration.ofMinutes(3), silentSince, true, NOW);

    // Act
    var resource = EquipmentResourceFromEntityAssembler.toResourceFromConnectivity(silent);

    // Assert
    assertThat(resource.state()).isEqualTo(ConnectivityStateResource.SILENT);
    assertThat(resource.silentSince()).isEqualTo(silentSince);
    assertThat(resource.underMaintenance()).isTrue();
  }
}
