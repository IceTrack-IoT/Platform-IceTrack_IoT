package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertPolicyUpdatedEvent;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class AlertPolicyTest {

  @Test
  @DisplayName("platformDefault() -> builds the active, equipment-less policy with the documented defaults (AAA)")
  void platformDefaultValues() {
    // Act
    var policy = AlertPolicy.platformDefault();

    // Assert
    assertThat(policy.isPlatformDefault()).isTrue();
    assertThat(policy.getEquipmentId()).isNull();
    assertThat(policy.isActive()).isTrue();
    assertThat(policy.getSustainedExcursionMinutes()).isEqualTo(AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES);
    assertThat(policy.getHysteresisMargin().celsius()).isEqualTo(AlertPolicy.DEFAULT_HYSTERESIS_MARGIN_CELSIUS);
    assertThat(policy.getMissedSyncWindowsForOffline())
        .isEqualTo(AlertPolicy.DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE);
  }

  @Test
  @DisplayName("AlertPolicy(EquipmentId, ...) -> builds an active per-equipment policy (AAA)")
  void equipmentPolicyValues() {
    // Act
    var policy = equipmentPolicy();

    // Assert
    assertThat(policy.isPlatformDefault()).isFalse();
    assertThat(policy.getEquipmentId()).isEqualTo(new EquipmentId(EQUIPMENT_ID));
    assertThat(policy.isActive()).isTrue();
    assertThat(policy.getSustainedExcursionMinutes()).isEqualTo(5);
    assertThat(policy.getHysteresisMargin().celsius()).isEqualTo(1.5);
    assertThat(policy.getMissedSyncWindowsForOffline()).isEqualTo(4);
  }

  @Test
  @DisplayName("updatePolicy(...) -> replaces the settings and announces them for an equipment policy (AAA)")
  void updatePolicyAnnouncesChange() {
    // Arrange
    var policy = equipmentPolicy();

    // Act
    policy.updatePolicy(10, 2.0, 6);

    // Assert
    assertThat(policy.getSustainedExcursionMinutes()).isEqualTo(10);
    assertThat(policy.getHysteresisMargin().celsius()).isEqualTo(2.0);
    assertThat(policy.getMissedSyncWindowsForOffline()).isEqualTo(6);
    assertThat(policy.domainEvents()).containsExactly(new AlertPolicyUpdatedEvent(EQUIPMENT_ID, 10, 2.0, 6));
  }

  @Test
  @DisplayName("updatePolicy(...) -> does not announce a change to the platform default (AAA)")
  void updatePlatformDefaultIsSilent() {
    var policy = AlertPolicy.platformDefault();

    policy.updatePolicy(3, 1.0, 5);

    assertThat(policy.domainEvents()).isEmpty();
    assertThat(policy.getSustainedExcursionMinutes()).isEqualTo(3);
  }

  @Test
  @DisplayName("deactivate() -> marks the policy inactive (AAA)")
  void deactivate() {
    var policy = equipmentPolicy();

    policy.deactivate();

    assertThat(policy.isActive()).isFalse();
  }

  @Test
  @DisplayName("assignId(Long) -> stores the persistence id (AAA)")
  void assignId() {
    var policy = equipmentPolicy();

    policy.assignId(9L);

    assertThat(policy.getId()).isEqualTo(9L);
  }

  @Test
  @DisplayName("offlineSilence() -> one 60 s sync window per tolerated missed window (AAA)")
  void offlineSilenceScalesWithWindows() {
    assertThat(equipmentPolicy().offlineSilence()).isEqualTo(Duration.ofSeconds(240));
    assertThat(AlertPolicy.platformDefault().offlineSilence()).isEqualTo(Duration.ofSeconds(180));
  }

  @ParameterizedTest(name = "[{index}] missedWindows={0}")
  @NullSource
  @ValueSource(ints = {0, -2})
  @DisplayName("offlineSilence() -> falls back to the default window count when unset or below one (AAA)")
  void offlineSilenceFallsBackToDefault(Integer missedWindows) {
    var policy = new AlertPolicy(null, 2, 1.0, missedWindows);

    assertThat(policy.offlineSilence())
        .isEqualTo(AlertPolicy.SYNC_WINDOW.multipliedBy(AlertPolicy.DEFAULT_MISSED_SYNC_WINDOWS_FOR_OFFLINE));
  }

  @Test
  @DisplayName("isOfflineSince(Instant, Instant) -> true exactly at the silence threshold and beyond (AAA)")
  void isOfflineAtThreshold() {
    // Arrange (default policy: 180 s)
    var policy = AlertPolicy.platformDefault();

    // Assert
    assertThat(policy.isOfflineSince(T0, T0.plusSeconds(179))).isFalse();
    assertThat(policy.isOfflineSince(T0, T0.plusSeconds(180))).isTrue();
    assertThat(policy.isOfflineSince(T0, T0.plusSeconds(600))).isTrue();
  }

  @Test
  @DisplayName("isOfflineSince(Instant, Instant) -> requires both instants (AAA)")
  void isOfflineRequiresInstants() {
    var policy = AlertPolicy.platformDefault();

    assertThrows(NullPointerException.class, () -> policy.isOfflineSince(null, Instant.now()));
    assertThrows(NullPointerException.class, () -> policy.isOfflineSince(T0, null));
  }
}
