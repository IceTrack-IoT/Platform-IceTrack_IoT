package pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertRaisedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertStatusChangedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class AlertTest {

  @Test
  @DisplayName("Alert.raisedFromEdge(...) -> opens a CRITICAL temperature alert at the detection instant (AAA)")
  void raisedFromEdgeOpensCriticalAlert() {
    // Act
    var alert = Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, TEMPERATURE, T0, Duration.ofMinutes(2));

    // Assert
    assertThat(alert.getId()).isNull();
    assertThat(alert.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(alert.getType()).isEqualTo(AlertType.TEMPERATURE_EXCURSION);
    assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    assertThat(alert.getTriggeringReadingId()).isEqualTo(READING_ID);
    assertThat(alert.getTriggeringTemperature().celsius()).isEqualTo(TEMPERATURE);
    assertThat(alert.getPeakTemperature().celsius()).isEqualTo(TEMPERATURE);
    assertThat(alert.getTriggeringRecordedAt()).isEqualTo(T0);
    assertThat(alert.getExcursionDuration()).isEqualTo(Duration.ofMinutes(2));
    assertThat(alert.getOpenedAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("Alert.raisedFromEdge(...) -> clamps an opening instant in the future to now (AAA)")
  void raisedFromEdgeClampsFutureDetection() {
    // Arrange
    var before = Instant.now();

    // Act
    var alert = Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, TEMPERATURE, before.plusSeconds(3600), null);

    // Assert
    assertThat(alert.getOpenedAt()).isBetween(before, Instant.now());
  }

  @Test
  @DisplayName("Alert.raisedFromEdge(...) -> rejects null temperature and null detection instant (AAA)")
  void raisedFromEdgeRequiresTemperatureAndInstant() {
    assertThrows(NullPointerException.class,
        () -> Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, null, T0, null));
    assertThrows(NullPointerException.class,
        () -> Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, TEMPERATURE, null, null));
  }

  @Test
  @DisplayName("Alert.raisedFromEdge(...) -> rejects an implausible temperature (AAA)")
  void raisedFromEdgeRejectsImplausibleTemperature() {
    assertThrows(IllegalArgumentException.class,
        () -> Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, 99.0, T0, null));
  }

  @Test
  @DisplayName("Alert.raisedForSilence(Long, Instant) -> opens a WARNING device-offline alert without temperatures (AAA)")
  void raisedForSilenceOpensWarning() {
    // Act
    var alert = Alert.raisedForSilence(EQUIPMENT_ID, T0);

    // Assert
    assertThat(alert.getType()).isEqualTo(AlertType.DEVICE_OFFLINE);
    assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    assertThat(alert.getTriggeringRecordedAt()).isEqualTo(T0);
    assertThat(alert.getTriggeringReadingId()).isNull();
    assertThat(alert.getTriggeringTemperature().celsius()).isNull();
    assertThat(alert.getPeakTemperature().celsius()).isNull();
    assertThat(alert.getExcursionDuration()).isNull();
  }

  @Test
  @DisplayName("Alert.raisedForSilence(Long, Instant) -> rejects a null silence start (AAA)")
  void raisedForSilenceRequiresInstant() {
    assertThrows(NullPointerException.class, () -> Alert.raisedForSilence(EQUIPMENT_ID, null));
  }

  @Test
  @DisplayName("Alert(...) -> rejects null equipment, type and severity (AAA)")
  void constructorRequiresMandatoryFields() {
    assertThrows(NullPointerException.class, () -> new Alert(null, AlertType.DEVICE_OFFLINE, AlertSeverity.INFO,
        null, null, T0, null, null));
    assertThrows(NullPointerException.class, () -> new Alert(EQUIPMENT_ID, null, AlertSeverity.INFO,
        null, null, T0, null, null));
    assertThrows(NullPointerException.class, () -> new Alert(EQUIPMENT_ID, AlertType.DEVICE_OFFLINE, null,
        null, null, T0, null, null));
  }

  @Test
  @DisplayName("onRaised() -> registers AlertRaisedEvent and an initial status announcement (AAA)")
  void onRaisedRegistersEvents() {
    // Arrange
    var alert = persistedTemperatureAlert();

    // Act
    alert.onRaised();

    // Assert
    assertThat(alert.domainEvents()).hasSize(2);
    var events = alert.domainEvents().stream().toList();
    assertThat(events.get(0)).isEqualTo(new AlertRaisedEvent(ALERT_ID, EQUIPMENT_ID,
        AlertType.TEMPERATURE_EXCURSION, AlertSeverity.CRITICAL, TEMPERATURE));
    assertThat(events.get(1)).isEqualTo(new AlertStatusChangedEvent(ALERT_ID, EQUIPMENT_ID,
        AlertType.TEMPERATURE_EXCURSION, AlertSeverity.CRITICAL, null, AlertStatus.OPEN, TEMPERATURE, T0));
  }

  @Test
  @DisplayName("assignId(Long) -> stores the persistence id (AAA)")
  void assignIdStoresId() {
    var alert = temperatureAlert();

    alert.assignId(ALERT_ID);

    assertThat(alert.getId()).isEqualTo(ALERT_ID);
  }

  @Test
  @DisplayName("acknowledge(Long) -> moves OPEN to ACKNOWLEDGED and records who and when (AAA)")
  void acknowledgeMovesToAcknowledged() {
    // Arrange
    var alert = persistedTemperatureAlert();
    var before = Instant.now();

    // Act
    alert.acknowledge(USER_ID);

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(alert.getAcknowledgedBy()).isEqualTo(USER_ID);
    assertThat(alert.getAcknowledgedAt()).isBetween(before, Instant.now());
    var event = (AlertStatusChangedEvent) alert.domainEvents().iterator().next();
    assertThat(event.previousStatus()).isEqualTo(AlertStatus.OPEN);
    assertThat(event.status()).isEqualTo(AlertStatus.ACKNOWLEDGED);
  }

  @Test
  @DisplayName("acknowledge(Long) -> requires an actor (AAA)")
  void acknowledgeRequiresActor() {
    var alert = persistedTemperatureAlert();

    assertThrows(IllegalArgumentException.class, () -> alert.acknowledge(null));
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
  }

  @Test
  @DisplayName("acknowledge(Long) -> is only allowed from OPEN (AAA)")
  void acknowledgeOnlyFromOpen() {
    // Arrange
    var alert = persistedTemperatureAlert();
    alert.acknowledge(USER_ID);

    // Act & Assert
    var ex = assertThrows(IllegalStateException.class, () -> alert.acknowledge(USER_ID));
    assertThat(ex).hasMessage("Only an OPEN alert can be acknowledged, was ACKNOWLEDGED");
  }

  @Test
  @DisplayName("resolve(Long) -> resolves an OPEN alert directly (AAA)")
  void resolveFromOpen() {
    // Arrange
    var alert = persistedTemperatureAlert();

    // Act
    alert.resolve(USER_ID);

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert.getResolvedBy()).isEqualTo(USER_ID);
    assertThat(alert.getResolvedAt()).isNotNull();
  }

  @Test
  @DisplayName("resolve(Long) -> resolves an ACKNOWLEDGED alert and announces the transition (AAA)")
  void resolveFromAcknowledged() {
    // Arrange
    var alert = persistedTemperatureAlert();
    alert.acknowledge(USER_ID);
    alert.clearDomainEvents();

    // Act
    alert.resolve(USER_ID);

    // Assert
    var event = (AlertStatusChangedEvent) alert.domainEvents().iterator().next();
    assertThat(event.previousStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
    assertThat(event.status()).isEqualTo(AlertStatus.RESOLVED);
  }

  @Test
  @DisplayName("resolve(Long) -> requires an actor and rejects an already closed alert (AAA)")
  void resolveGuards() {
    // Arrange
    var alert = persistedTemperatureAlert();
    assertThrows(IllegalArgumentException.class, () -> alert.resolve(null));
    alert.resolve(USER_ID);

    // Act & Assert
    var ex = assertThrows(IllegalStateException.class, () -> alert.resolve(USER_ID));
    assertThat(ex).hasMessage("Alert already closed with status RESOLVED");
  }

  @Test
  @DisplayName("resolveAutomatically() -> resolves without an actor (AAA)")
  void resolveAutomatically() {
    // Arrange
    var alert = persistedOfflineAlert();

    // Act
    alert.resolveAutomatically();

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert.getResolvedBy()).isNull();
    assertThat(alert.getResolvedAt()).isNotNull();
  }

  @Test
  @DisplayName("resolveAutomatically() -> rejects an alert that is already closed (AAA)")
  void resolveAutomaticallyOnClosed() {
    var alert = persistedOfflineAlert();
    alert.resolveAutomatically();

    assertThrows(IllegalStateException.class, alert::resolveAutomatically);
  }

  @Test
  @DisplayName("resolveFromEdge(Double, Instant) -> resolves at the normalization instant and records the peak (AAA)")
  void resolveFromEdge() {
    // Arrange
    var alert = persistedTemperatureAlert();
    var normalizedAt = T0.plusSeconds(300);

    // Act
    alert.resolveFromEdge(10.5, normalizedAt);

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert.getResolvedAt()).isEqualTo(normalizedAt);
    assertThat(alert.getPeakTemperature().celsius()).isEqualTo(10.5);
    assertThat(alert.getResolvedBy()).isNull();
  }

  @Test
  @DisplayName("resolveFromEdge(Double, Instant) -> keeps the previous peak when none is reported (AAA)")
  void resolveFromEdgeKeepsPeak() {
    var alert = persistedTemperatureAlert();

    alert.resolveFromEdge(null, T0.plusSeconds(60));

    assertThat(alert.getPeakTemperature().celsius()).isEqualTo(TEMPERATURE);
  }

  @Test
  @DisplayName("resolveFromEdge(Double, Instant) -> never resolves before the alert opened (AAA)")
  void resolveFromEdgeNotBeforeOpening() {
    var alert = persistedTemperatureAlert();

    alert.resolveFromEdge(null, T0.minusSeconds(500));

    assertThat(alert.getResolvedAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("resolveFromEdge(Double, Instant) -> never resolves in the future (AAA)")
  void resolveFromEdgeNotInFuture() {
    var alert = persistedTemperatureAlert();
    var before = Instant.now();

    alert.resolveFromEdge(null, before.plusSeconds(3600));

    assertThat(alert.getResolvedAt()).isBetween(before, Instant.now());
  }

  @Test
  @DisplayName("resolveFromEdge(Double, Instant) -> requires the normalization instant and an active alert (AAA)")
  void resolveFromEdgeGuards() {
    var alert = persistedTemperatureAlert();
    assertThrows(NullPointerException.class, () -> alert.resolveFromEdge(null, null));
    alert.resolveFromEdge(null, T0.plusSeconds(1));

    assertThrows(IllegalStateException.class, () -> alert.resolveFromEdge(null, T0.plusSeconds(2)));
  }

  @Test
  @DisplayName("dismiss(Long, String, String) -> dismisses with trimmed reason and evidence (AAA)")
  void dismissStoresTrimmedText() {
    // Arrange
    var alert = persistedTemperatureAlert();

    // Act
    alert.dismiss(USER_ID, "  Sensor drift  ", "  photo-001 ");

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.DISMISSED);
    assertThat(alert.getDismissedBy()).isEqualTo(USER_ID);
    assertThat(alert.getDismissedAt()).isNotNull();
    assertThat(alert.getDismissReason()).isEqualTo("Sensor drift");
    assertThat(alert.getDismissEvidence()).isEqualTo("photo-001");
  }

  @ParameterizedTest(name = "[{index}] evidence=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("dismiss(Long, String, String) -> a blank evidence is stored as null (AAA)")
  void dismissBlankEvidenceIsNull(String evidence) {
    var alert = persistedTemperatureAlert();

    alert.dismiss(USER_ID, "reason", evidence);

    assertThat(alert.getDismissEvidence()).isNull();
  }

  @ParameterizedTest(name = "[{index}] reason=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {"   "})
  @DisplayName("dismiss(Long, String, String) -> requires a reason (AAA)")
  void dismissRequiresReason(String reason) {
    var alert = persistedTemperatureAlert();

    var ex = assertThrows(IllegalArgumentException.class, () -> alert.dismiss(USER_ID, reason, null));

    assertThat(ex).hasMessage("A reason is required to dismiss an alert");
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
  }

  @Test
  @DisplayName("dismiss(Long, String, String) -> requires an actor (AAA)")
  void dismissRequiresActor() {
    var alert = persistedTemperatureAlert();

    assertThrows(IllegalArgumentException.class, () -> alert.dismiss(null, "reason", null));
  }

  @Test
  @DisplayName("dismiss(Long, String, String) -> accepts a reason of exactly the maximum length and rejects one more (AAA)")
  void dismissReasonLengthBoundary() {
    // Arrange
    var alert = persistedTemperatureAlert();
    var exact = "r".repeat(Alert.MAX_DISMISS_REASON_LENGTH);
    var tooLong = "r".repeat(Alert.MAX_DISMISS_REASON_LENGTH + 1);

    // Act & Assert
    var ex = assertThrows(IllegalArgumentException.class, () -> alert.dismiss(USER_ID, tooLong, null));
    assertThat(ex).hasMessage("The dismissal reason must not exceed 500 characters");
    alert.dismiss(USER_ID, exact, null);
    assertThat(alert.getDismissReason()).hasSize(Alert.MAX_DISMISS_REASON_LENGTH);
  }

  @Test
  @DisplayName("dismiss(Long, String, String) -> rejects evidence longer than the maximum (AAA)")
  void dismissEvidenceTooLong() {
    var alert = persistedTemperatureAlert();
    var tooLong = "e".repeat(Alert.MAX_DISMISS_EVIDENCE_LENGTH + 1);

    var ex = assertThrows(IllegalArgumentException.class, () -> alert.dismiss(USER_ID, "reason", tooLong));

    assertThat(ex).hasMessage("The dismissal evidence must not exceed 1000 characters");
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
  }

  @Test
  @DisplayName("dismiss(Long, String, String) -> rejects an alert that is already closed (AAA)")
  void dismissClosedAlert() {
    var alert = persistedTemperatureAlert();
    alert.resolve(USER_ID);

    var ex = assertThrows(IllegalStateException.class, () -> alert.dismiss(USER_ID, "reason", null));

    assertThat(ex).hasMessage("Alert already closed with status RESOLVED");
  }

  @Test
  @DisplayName("totalDuration() -> null while the alert is still active (AAA)")
  void totalDurationNullWhileActive() {
    assertThat(persistedTemperatureAlert().totalDuration()).isNull();
  }

  @Test
  @DisplayName("totalDuration() -> time between opening and resolution (AAA)")
  void totalDurationWhenResolved() {
    var alert = persistedTemperatureAlert();
    alert.resolveFromEdge(null, T0.plusSeconds(300));

    assertThat(alert.totalDuration()).isEqualTo(Duration.ofSeconds(300));
  }

  @Test
  @DisplayName("totalDuration() -> time between opening and dismissal (AAA)")
  void totalDurationWhenDismissed() {
    var alert = persistedTemperatureAlert();
    alert.dismiss(USER_ID, "reason", null);

    assertThat(alert.totalDuration()).isEqualTo(Duration.between(T0, alert.getDismissedAt()));
  }

  @Test
  @DisplayName("restoreState(...) -> rehydrates every lifecycle field (AAA)")
  void restoreStateRehydrates() {
    // Arrange
    var alert = temperatureAlert();
    var opened = T0.minusSeconds(100);

    // Act
    alert.restoreState(AlertStatus.DISMISSED, opened, 1L, T0, 2L, T0.plusSeconds(1), 3L, T0.plusSeconds(2),
        "reason", "evidence");

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.DISMISSED);
    assertThat(alert.getOpenedAt()).isEqualTo(opened);
    assertThat(alert.getAcknowledgedBy()).isEqualTo(1L);
    assertThat(alert.getResolvedBy()).isEqualTo(2L);
    assertThat(alert.getDismissedBy()).isEqualTo(3L);
    assertThat(alert.getDismissReason()).isEqualTo("reason");
    assertThat(alert.getDismissEvidence()).isEqualTo("evidence");
    assertThat(alert.domainEvents()).isEmpty();
  }

  @Test
  @DisplayName("restoreState(...) -> requires status and opening instant (AAA)")
  void restoreStateRequiresStatusAndOpenedAt() {
    var alert = temperatureAlert();

    assertThrows(NullPointerException.class,
        () -> alert.restoreState(null, T0, null, null, null, null, null, null, null, null));
    assertThrows(NullPointerException.class,
        () -> alert.restoreState(AlertStatus.OPEN, null, null, null, null, null, null, null, null, null));
  }
}
