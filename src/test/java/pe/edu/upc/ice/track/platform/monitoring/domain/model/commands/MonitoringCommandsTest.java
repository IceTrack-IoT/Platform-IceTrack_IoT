package pe.edu.upc.ice.track.platform.monitoring.domain.model.commands;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

class MonitoringCommandsTest {

  @ParameterizedTest(name = "[{index}] reason=''{0}''")
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  @DisplayName("DismissAlertCommand(...) -> requires a reason (AAA)")
  void dismissRequiresReason(String reason) {
    var ex = assertThrows(IllegalArgumentException.class,
        () -> new DismissAlertCommand(ALERT_ID, USERNAME, reason, null));
    assertThat(ex).hasMessage("A reason is required to dismiss an alert");
  }

  @Test
  @DisplayName("DismissAlertCommand(...) -> evidence is optional (AAA)")
  void dismissEvidenceOptional() {
    assertDoesNotThrow(() -> new DismissAlertCommand(ALERT_ID, USERNAME, "reason", null));
  }

  @Test
  @DisplayName("DetectOfflineEquipmentCommand(Instant) -> requires the detection instant (AAA)")
  void detectOfflineRequiresInstant() {
    assertThrows(NullPointerException.class, () -> new DetectOfflineEquipmentCommand(null));
    assertThat(new DetectOfflineEquipmentCommand(T0).detectedAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("RecordEdgeEventCommand(...) -> the fixtures for each event type are valid (AAA)")
  void edgeEventFixturesAreValid() {
    assertDoesNotThrow(() -> excursionDetectedCommand());
    assertDoesNotThrow(() -> excursionNormalizedCommand());
    assertDoesNotThrow(() -> maintenanceStartedCommand(MaintenanceOrigin.BUTTON));
    assertDoesNotThrow(() -> maintenanceEndedCommand(ClosureReason.MANUAL_APP));
  }

  @Test
  @DisplayName("RecordEdgeEventCommand(...) -> requires uid, type, device, equipment and instant (AAA)")
  void edgeEventRequiresIdentity() {
    var type = EdgeEventType.THERMAL_EXCURSION_NORMALIZED;
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(null, type, DEVICE_ID,
        EQUIPMENT_ID, T0, null, null, null, null, null, null, null, null));
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID, null, DEVICE_ID,
        EQUIPMENT_ID, T0, null, null, null, null, null, null, null, null));
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID, type, null,
        EQUIPMENT_ID, T0, null, null, null, null, null, null, null, null));
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID, type, DEVICE_ID,
        null, T0, null, null, null, null, null, null, null, null));
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID, type, DEVICE_ID,
        EQUIPMENT_ID, null, null, null, null, null, null, null, null, null));
  }

  @Test
  @DisplayName("RecordEdgeEventCommand(...) -> THERMAL_EXCURSION_DETECTED requires a temperature (AAA)")
  void detectedRequiresTemperature() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID,
        EdgeEventType.THERMAL_EXCURSION_DETECTED, DEVICE_ID, EQUIPMENT_ID, T0, null, null, null, null, null, null,
        null, null));
    assertThat(ex).hasMessage("temperature_celsius is required for THERMAL_EXCURSION_DETECTED");
  }

  @Test
  @DisplayName("RecordEdgeEventCommand(...) -> MAINTENANCE_WINDOW_STARTED requires an origin (AAA)")
  void startedRequiresOrigin() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID,
        EdgeEventType.MAINTENANCE_WINDOW_STARTED, DEVICE_ID, EQUIPMENT_ID, T0, null, null, null, null, null, null,
        null, null));
    assertThat(ex).hasMessage("origin is required for MAINTENANCE_WINDOW_STARTED");
  }

  @Test
  @DisplayName("RecordEdgeEventCommand(...) -> MAINTENANCE_WINDOW_ENDED requires a closure reason (AAA)")
  void endedRequiresReason() {
    var ex = assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID,
        EdgeEventType.MAINTENANCE_WINDOW_ENDED, DEVICE_ID, EQUIPMENT_ID, T0, null, null, null, null, null, null,
        null, null));
    assertThat(ex).hasMessage("closure_reason is required for MAINTENANCE_WINDOW_ENDED");
  }

  @ParameterizedTest(name = "[{index}] id={0}")
  @ValueSource(longs = {0, -3})
  @DisplayName("RecordEdgeEventCommand(...) -> technician and service request ids must be positive when sent (AAA)")
  void idsMustBePositive(long id) {
    var type = EdgeEventType.THERMAL_EXCURSION_NORMALIZED;
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID, type, DEVICE_ID,
        EQUIPMENT_ID, T0, null, null, null, null, id, null, null, null));
    assertThrows(IllegalArgumentException.class, () -> new RecordEdgeEventCommand(EVENT_UID, type, DEVICE_ID,
        EQUIPMENT_ID, T0, null, null, null, null, null, id, null, null));
  }

  @Test
  @DisplayName("RecordEdgeEventCommand(...) -> NORMALIZED needs neither temperature, origin nor reason (AAA)")
  void normalizedNeedsNothingElse() {
    assertDoesNotThrow(() -> new RecordEdgeEventCommand(UUID.randomUUID(), EdgeEventType.THERMAL_EXCURSION_NORMALIZED,
        DEVICE_ID, EQUIPMENT_ID, Instant.now(), null, null, null, null, null, null, null, null));
  }
}
