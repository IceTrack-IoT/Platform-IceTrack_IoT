package pe.edu.upc.ice.track.platform.monitoring.test.fixtures;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.*;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Canonical constants and domain object factories for the monitoring context. Every factory returns
 * a fresh, valid instance. Tests MUST NOT build monitoring aggregates inline.
 */
public final class MonitoringDomainFixtures {

  private MonitoringDomainFixtures() {
    throw new AssertionError("MonitoringDomainFixtures is a utility class and must not be instantiated.");
  }

  public static final Long EQUIPMENT_ID = 7L;
  public static final Long OTHER_EQUIPMENT_ID = 8L;
  public static final Long DEVICE_ID = 3L;
  public static final Long ALERT_ID = 100L;
  public static final Long READING_ID = 200L;
  public static final Long WINDOW_ID = 300L;
  public static final Long EDGE_EVENT_ID = 400L;
  public static final Long USER_ID = 10L;
  public static final Long OWNER_PROFILE_ID = 55L;
  public static final Long TECHNICIAN_PROFILE_ID = 66L;
  public static final Long SERVICE_REQUEST_ID = 77L;
  public static final Long SITE_ID = 5L;
  public static final String USERNAME = "jperez";

  public static final UUID READING_UID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  public static final UUID EVENT_UID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  public static final UUID START_EVENT_UID = UUID.fromString("33333333-3333-3333-3333-333333333333");

  public static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

  public static final double TEMPERATURE = 8.5;
  public static final double HYSTERESIS = 1.0;

  /** An unsaved, OPEN temperature-excursion alert raised from the edge at {@link #T0}. */
  public static Alert temperatureAlert() {
    return Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, TEMPERATURE, T0, Duration.ofMinutes(2));
  }

  /** The same alert with a persistence id, as the repository would return it. */
  public static Alert persistedTemperatureAlert() {
    var alert = temperatureAlert();
    alert.assignId(ALERT_ID);
    return alert;
  }

  /** An OPEN device-offline alert with a persistence id. */
  public static Alert persistedOfflineAlert() {
    var alert = Alert.raisedForSilence(EQUIPMENT_ID, T0);
    alert.assignId(ALERT_ID);
    return alert;
  }

  public static Alert persistedAlert(AlertType type, AlertSeverity severity) {
    var alert = new Alert(EQUIPMENT_ID, type, severity, null, TEMPERATURE, T0, TEMPERATURE, null);
    alert.assignId(ALERT_ID);
    return alert;
  }

  public static AlertPolicy equipmentPolicy() {
    return new AlertPolicy(new EquipmentId(EQUIPMENT_ID), 5, 1.5, 4);
  }

  public static AlertPolicy persistedEquipmentPolicy() {
    var policy = equipmentPolicy();
    policy.assignId(1L);
    return policy;
  }

  /** An unsaved, active BUTTON window started at {@link #T0}. */
  public static MaintenanceWindow buttonWindow() {
    return MaintenanceWindow.open(START_EVENT_UID, EQUIPMENT_ID, MaintenanceOrigin.BUTTON, T0, null, null);
  }

  public static MaintenanceWindow persistedButtonWindow() {
    var window = buttonWindow();
    window.assignId(WINDOW_ID);
    return window;
  }

  /** An unsaved, active APP window linked to a technician and service request. */
  public static MaintenanceWindow appWindow() {
    return MaintenanceWindow.open(START_EVENT_UID, EQUIPMENT_ID, MaintenanceOrigin.APP, T0,
        TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID);
  }

  public static MaintenanceWindow persistedAppWindow() {
    var window = appWindow();
    window.assignId(WINDOW_ID);
    return window;
  }

  public static MaintenanceWindow closedWindow() {
    return new MaintenanceWindow(WINDOW_ID, START_EVENT_UID, EQUIPMENT_ID, MaintenanceOrigin.BUTTON, T0,
        T0.plus(Duration.ofMinutes(30)), T0.plusSeconds(600), ClosureReason.MANUAL_BUTTON, null, null);
  }

  public static SensorReading sensorReading() {
    return sensorReading(T0, 4.0, 5.0, 6.0, 40, false);
  }

  public static SensorReading sensorReading(Instant recordedAt, double min, double avg, double max,
                                            int sampleCount, boolean underMaintenance) {
    return SensorReading.record(READING_UID, new EquipmentId(EQUIPMENT_ID), DEVICE_ID,
        min, max, avg, min, max, avg, 55.0, sampleCount,
        new EnvironmentObservation(10.0, 12, true), recordedAt, 1L, underMaintenance);
  }

  public static EdgeEvent edgeEvent(EdgeEventType type, EdgeEventResult result) {
    return new EdgeEvent(EDGE_EVENT_ID, EVENT_UID, DEVICE_ID, EQUIPMENT_ID, type, T0, TEMPERATURE, TEMPERATURE,
        result, null, null);
  }
}
