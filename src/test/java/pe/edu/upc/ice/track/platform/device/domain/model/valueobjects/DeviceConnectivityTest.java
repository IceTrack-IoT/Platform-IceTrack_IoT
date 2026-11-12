package pe.edu.upc.ice.track.platform.device.domain.model.valueobjects;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.device.test.fixtures.DeviceDomainFixtures;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeviceConnectivityTest {

  private static final Instant NOW = Instant.parse("2026-01-01T10:10:00Z");
  private static final Duration WINDOW = Duration.ofMinutes(3);

  private static DeviceConnectivity evaluatePairedSeenAt(Instant seenAt, Duration window) {
    var device = DeviceDomainFixtures.deviceBuilder().paired().lastSeenAt(seenAt).build();
    return DeviceConnectivity.evaluate(device, window, NOW);
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> UNPAIRED device is UNPAIRED with no window (AAA)")
  void evaluateUnpaired() {
    // Act
    var result = DeviceConnectivity.evaluate(DeviceDomainFixtures.unpairedDevice(), WINDOW, NOW);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.UNPAIRED);
    assertThat(result.silentAfter()).isNull();
    assertThat(result.evaluatedAt()).isEqualTo(NOW);
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> REVOKED device is KEY_REVOKED and keeps the window (AAA)")
  void evaluateRevoked() {
    // Act
    var result = DeviceConnectivity.evaluate(DeviceDomainFixtures.revokedDevice(), WINDOW, NOW);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.KEY_REVOKED);
    assertThat(result.silentAfter()).isEqualTo(WINDOW);
    assertThat(result.silentSince()).isNull();
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> a paired device that never reported is NEVER_REPORTED (AAA)")
  void evaluatePairedNeverReported() {
    // Act
    var result = evaluatePairedSeenAt(null, WINDOW);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.NEVER_REPORTED);
    assertThat(result.silentAfter()).isEqualTo(WINDOW);
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> a paired device seen within the window is REPORTING (AAA)")
  void evaluatePairedReporting() {
    // Act
    var result = evaluatePairedSeenAt(NOW.minus(Duration.ofMinutes(1)), WINDOW);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.REPORTING);
    assertThat(result.silentSince()).isNull();
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> a paired device silent past the window is SILENT since lastSeen + window (AAA)")
  void evaluatePairedSilent() {
    // Arrange
    var seenAt = NOW.minus(Duration.ofMinutes(10));

    // Act
    var result = evaluatePairedSeenAt(seenAt, WINDOW);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.SILENT);
    assertThat(result.silentSince()).isEqualTo(seenAt.plus(WINDOW));
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> exactly at the window boundary the device is still REPORTING (AAA)")
  void evaluatePairedAtBoundary() {
    // Act
    var result = evaluatePairedSeenAt(NOW.minus(WINDOW), WINDOW);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.REPORTING);
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> without a window a paired device that reported is REPORTING (AAA)")
  void evaluatePairedWithoutWindow() {
    // Act
    var result = evaluatePairedSeenAt(NOW.minus(Duration.ofDays(30)), null);

    // Assert
    assertThat(result.state()).isEqualTo(DeviceConnectivityState.REPORTING);
    assertThat(result.silentAfter()).isNull();
  }

  @Test
  @DisplayName("evaluate(Device, Duration, Instant) -> NullPointerException for a null device or instant (AAA)")
  void evaluateRejectsNulls() {
    // Act & Assert
    assertThrows(NullPointerException.class, () -> DeviceConnectivity.evaluate(null, WINDOW, NOW));
    assertThrows(NullPointerException.class,
        () -> DeviceConnectivity.evaluate(DeviceDomainFixtures.pairedDevice(), WINDOW, null));
  }

  // ---------- constructor ----------

  @Test
  @DisplayName("DeviceConnectivity(...) -> silentSince is discarded unless the state is SILENT (AAA)")
  void constructorDropsSilentSinceWhenNotSilent() {
    // Act
    var result = new DeviceConnectivity(DeviceConnectivityState.REPORTING, WINDOW, NOW, NOW);

    // Assert
    assertThat(result.silentSince()).isNull();
  }

  @Test
  @DisplayName("DeviceConnectivity(...) -> rejects a null state or evaluation instant (AAA)")
  void constructorRejectsNulls() {
    // Act & Assert
    assertThrows(NullPointerException.class, () -> new DeviceConnectivity(null, WINDOW, null, NOW));
    assertThrows(NullPointerException.class,
        () -> new DeviceConnectivity(DeviceConnectivityState.REPORTING, WINDOW, null, null));
  }

  // ---------- DevicePage ----------

  @Test
  @DisplayName("DevicePage(...) -> rejects null content, negative page and non positive size (AAA)")
  void devicePageValidation() {
    // Act & Assert
    assertThrows(IllegalArgumentException.class, () -> new DevicePage(null, 0, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> new DevicePage(List.of(), -1, 10, 0));
    assertThrows(IllegalArgumentException.class, () -> new DevicePage(List.of(), 0, 0, 0));
  }

  @Test
  @DisplayName("DevicePage.unpaged(List) -> wraps everything in a single page of at least one row (AAA)")
  void devicePageUnpaged() {
    // Act
    var empty = DevicePage.unpaged(List.of());
    var one = DevicePage.unpaged(List.of(DeviceDomainFixtures.pairedDevice()));

    // Assert
    assertThat(empty.size()).isEqualTo(1);
    assertThat(empty.totalPages()).isEqualTo(1);
    assertThat(one.size()).isEqualTo(1);
    assertThat(one.totalElements()).isEqualTo(1);
  }

  @Test
  @DisplayName("DevicePage.empty(int, int) / totalPages() -> empty page has no content and rounds pages up (AAA)")
  void devicePageEmptyAndTotalPages() {
    // Act
    var empty = DevicePage.empty(2, 20);
    var page = new DevicePage(List.of(), 0, 10, 25);

    // Assert
    assertThat(empty.content()).isEmpty();
    assertThat(empty.page()).isEqualTo(2);
    assertThat(empty.totalPages()).isEqualTo(1);
    assertThat(page.totalPages()).isEqualTo(3);
  }
}
