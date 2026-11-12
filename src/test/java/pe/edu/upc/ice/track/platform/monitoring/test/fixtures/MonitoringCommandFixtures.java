package pe.edu.upc.ice.track.platform.monitoring.test.fixtures;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.*;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;

import java.time.Instant;

import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

public final class MonitoringCommandFixtures {

  private MonitoringCommandFixtures() {
    throw new AssertionError("MonitoringCommandFixtures is a utility class and must not be instantiated.");
  }

  public static AcknowledgeAlertCommand acknowledgeAlertCommand() {
    return new AcknowledgeAlertCommand(ALERT_ID, USERNAME);
  }

  public static ResolveAlertCommand resolveAlertCommand() {
    return new ResolveAlertCommand(ALERT_ID, USERNAME);
  }

  public static DismissAlertCommand dismissAlertCommand() {
    return new DismissAlertCommand(ALERT_ID, USERNAME, "Sensor drift", "photo-001");
  }

  public static UpdateAlertPolicyCommand updateAlertPolicyCommand() {
    return new UpdateAlertPolicyCommand(EQUIPMENT_ID, USERNAME, 5, 1.5, 4);
  }

  public static RecordReadingBatchCommand recordReadingBatchCommand() {
    return recordReadingBatchCommand(T0);
  }

  public static RecordReadingBatchCommand recordReadingBatchCommand(Instant recordedAt) {
    return new RecordReadingBatchCommand(READING_UID, EQUIPMENT_ID, DEVICE_ID,
        4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 55.0, 40, 10.0, 12, true, recordedAt, 1L);
  }

  public static RecordEdgeEventCommand excursionDetectedCommand() {
    return new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.THERMAL_EXCURSION_DETECTED, DEVICE_ID,
        EQUIPMENT_ID, T0, TEMPERATURE, TEMPERATURE, READING_UID, null, null, null, null, null);
  }

  public static RecordEdgeEventCommand excursionNormalizedCommand() {
    return new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.THERMAL_EXCURSION_NORMALIZED, DEVICE_ID,
        EQUIPMENT_ID, T0, 4.0, 9.0, null, null, null, null, null, null);
  }

  public static RecordEdgeEventCommand maintenanceStartedCommand(MaintenanceOrigin origin) {
    return new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.MAINTENANCE_WINDOW_STARTED, DEVICE_ID,
        EQUIPMENT_ID, T0, null, null, null, origin, TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID, null, null);
  }

  public static RecordEdgeEventCommand maintenanceEndedCommand(ClosureReason reason) {
    return new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.MAINTENANCE_WINDOW_ENDED, DEVICE_ID,
        EQUIPMENT_ID, T0.plusSeconds(600), null, null, null, null, null, null, reason, START_EVENT_UID);
  }
}
