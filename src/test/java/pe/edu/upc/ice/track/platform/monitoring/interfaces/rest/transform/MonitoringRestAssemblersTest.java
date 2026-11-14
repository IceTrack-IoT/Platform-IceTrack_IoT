package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.monitoring.application.results.IssuedStreamTicket;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertStatusChangedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.ReadingRecordedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.readmodels.DashboardSummary;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.AuthenticatedDevice;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

/** The assemblers are pure mappings, so each case builds real domain objects and checks every mapped field. */
class MonitoringRestAssemblersTest {

  @Nested
  class AlertAssemblers {

    @Test
    @DisplayName("AlertResourceFromEntityAssembler.toResourceFromEntity(open alert) -> maps fields, null duration (AAA)")
    void mapsOpenAlert() {
      // Arrange
      var alert = persistedTemperatureAlert();

      // Act
      var resource = AlertResourceFromEntityAssembler.toResourceFromEntity(alert);

      // Assert
      assertThat(resource.id()).isEqualTo(ALERT_ID);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.type()).isEqualTo("TEMPERATURE_EXCURSION");
      assertThat(resource.severity()).isEqualTo("CRITICAL");
      assertThat(resource.status()).isEqualTo("OPEN");
      assertThat(resource.triggeringReadingId()).isEqualTo(READING_ID);
      assertThat(resource.triggeringTemperature()).isEqualTo(TEMPERATURE);
      assertThat(resource.triggeringRecordedAt()).isEqualTo(T0);
      assertThat(resource.peakTemperature()).isEqualTo(TEMPERATURE);
      assertThat(resource.openedAt()).isEqualTo(alert.getOpenedAt());
      assertThat(resource.acknowledgedBy()).isNull();
      assertThat(resource.resolvedAt()).isNull();
      assertThat(resource.dismissedAt()).isNull();
      assertThat(resource.totalDurationSeconds()).isNull();
    }

    @Test
    @DisplayName("AlertResourceFromEntityAssembler.toResourceFromEntity(dismissed alert) -> maps dismissal and duration (AAA)")
    void mapsDismissedAlert() {
      // Arrange
      var alert = persistedTemperatureAlert();
      alert.restoreState(AlertStatus.DISMISSED, T0, USER_ID, T0.plusSeconds(60), null, null,
          USER_ID, T0.plusSeconds(780), "Planned defrost", "photo-001");

      // Act
      var resource = AlertResourceFromEntityAssembler.toResourceFromEntity(alert);

      // Assert
      assertThat(resource.status()).isEqualTo("DISMISSED");
      assertThat(resource.acknowledgedBy()).isEqualTo(USER_ID);
      assertThat(resource.dismissedBy()).isEqualTo(USER_ID);
      assertThat(resource.dismissReason()).isEqualTo("Planned defrost");
      assertThat(resource.dismissEvidence()).isEqualTo("photo-001");
      assertThat(resource.totalDurationSeconds()).isEqualTo(780L);
    }

    @Test
    @DisplayName("AlertResourceFromEntityAssembler.toPagedResourceFromPage(page) -> maps content and paging figures (AAA)")
    void mapsPage() {
      // Arrange
      var page = new AlertPage(List.of(persistedTemperatureAlert()), 1, 20, 41);

      // Act
      var resource = AlertResourceFromEntityAssembler.toPagedResourceFromPage(page);

      // Assert
      assertThat(resource.content()).hasSize(1);
      assertThat(resource.page()).isEqualTo(1);
      assertThat(resource.size()).isEqualTo(20);
      assertThat(resource.totalElements()).isEqualTo(41);
      assertThat(resource.totalPages()).isEqualTo(3);
    }

    @Test
    @DisplayName("AlertResourceFromEntityAssembler.toPagedResourceFromPage(empty page) -> one empty page (AAA)")
    void mapsEmptyPage() {
      // Arrange
      var page = new AlertPage(List.of(), 0, 20, 0);

      // Act
      var resource = AlertResourceFromEntityAssembler.toPagedResourceFromPage(page);

      // Assert
      assertThat(resource.content()).isEmpty();
      assertThat(resource.totalPages()).isEqualTo(1);
    }
  }

  @Nested
  class PolicyAssemblers {

    @Test
    @DisplayName("AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(equipment policy) -> carries the equipment id (AAA)")
    void mapsEquipmentPolicy() {
      // Arrange & Act
      var resource = AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(persistedEquipmentPolicy());

      // Assert
      assertThat(resource.id()).isEqualTo(1L);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.sustainedExcursionMinutes()).isEqualTo(5);
      assertThat(resource.hysteresisMarginCelsius()).isEqualTo(1.5);
      assertThat(resource.missedSyncWindowsForOffline()).isEqualTo(4);
      assertThat(resource.active()).isTrue();
    }

    @Test
    @DisplayName("AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(platform default) -> equipment id is null (AAA)")
    void mapsPlatformDefaultPolicy() {
      // Arrange & Act
      var resource = AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(AlertPolicy.platformDefault());

      // Assert
      assertThat(resource.equipmentId()).isNull();
      assertThat(resource.sustainedExcursionMinutes()).isEqualTo(AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES);
    }

    @Test
    @DisplayName("ApplicablePolicyResourceFromEntityAssembler.toResourcesFromEntities(list) -> source EQUIPMENT or DEFAULT (AAA)")
    void mapsApplicablePolicies() {
      // Arrange
      var own = new ApplicableAlertPolicy(EQUIPMENT_ID, persistedEquipmentPolicy());
      var fallback = new ApplicableAlertPolicy(OTHER_EQUIPMENT_ID, AlertPolicy.platformDefault());

      // Act
      var resources = ApplicablePolicyResourceFromEntityAssembler.toResourcesFromEntities(List.of(own, fallback));

      // Assert
      assertThat(resources).hasSize(2);
      assertThat(resources.get(0).equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resources.get(0).source()).isEqualTo("EQUIPMENT");
      assertThat(resources.get(0).policy().equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resources.get(1).equipmentId()).isEqualTo(OTHER_EQUIPMENT_ID);
      assertThat(resources.get(1).source()).isEqualTo("DEFAULT");
      assertThat(resources.get(1).policy().equipmentId()).isNull();
    }
  }

  @Nested
  class DashboardAssembler {

    @Test
    @DisplayName("DashboardSummaryResourceFromEntityAssembler.toResourceFromEntity(summary) -> maps every figure group (AAA)")
    void mapsSummary() {
      // Arrange
      var summary = new DashboardSummary(T0, 3,
          new DashboardSummary.EquipmentFigures(4, Map.of("ONLINE", 3L, "OFFLINE", 1L), 2, 1, 1),
          new DashboardSummary.AlertFigures(1, 2, Map.of(AlertSeverity.CRITICAL, 1L, AlertSeverity.WARNING, 2L)),
          new DashboardSummary.ServiceRequestFigures(5, Map.of("PENDING", 5L)),
          new DashboardSummary.DeviceFigures(6, 1));

      // Act
      var resource = DashboardSummaryResourceFromEntityAssembler.toResourceFromEntity(summary);

      // Assert
      assertThat(resource.generatedAt()).isEqualTo(T0);
      assertThat(resource.siteCount()).isEqualTo(3);
      assertThat(resource.equipment().total()).isEqualTo(4);
      assertThat(resource.equipment().byStatus()).containsEntry("ONLINE", 3L).containsEntry("OFFLINE", 1L);
      assertThat(resource.equipment().reporting()).isEqualTo(2);
      assertThat(resource.equipment().silent()).isEqualTo(1);
      assertThat(resource.equipment().neverReported()).isEqualTo(1);
      assertThat(resource.alerts().open()).isEqualTo(1);
      assertThat(resource.alerts().acknowledged()).isEqualTo(2);
      assertThat(resource.alerts().activeBySeverity()).containsEntry("CRITICAL", 1L).containsEntry("WARNING", 2L);
      assertThat(resource.serviceRequests().active()).isEqualTo(5);
      assertThat(resource.serviceRequests().byStatus()).containsEntry("PENDING", 5L);
      assertThat(resource.devices().paired()).isEqualTo(6);
      assertThat(resource.devices().revoked()).isEqualTo(1);
    }
  }

  @Nested
  class MaintenanceWindowAssembler {

    @Test
    @DisplayName("MaintenanceWindowResourceFromEntityAssembler.toResourceFromEntity(active unlinked) -> active and unlinked (AAA)")
    void mapsActiveUnlinkedWindow() {
      // Arrange & Act
      var resource = MaintenanceWindowResourceFromEntityAssembler.toResourceFromEntity(persistedButtonWindow());

      // Assert
      assertThat(resource.id()).isEqualTo(WINDOW_ID);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.origin()).isEqualTo("BUTTON");
      assertThat(resource.startedAt()).isEqualTo(T0);
      assertThat(resource.expiresAt()).isEqualTo(T0.plus(Duration.ofMinutes(30)));
      assertThat(resource.endedAt()).isNull();
      assertThat(resource.closureReason()).isNull();
      assertThat(resource.unlinked()).isTrue();
      assertThat(resource.active()).isTrue();
    }

    @Test
    @DisplayName("MaintenanceWindowResourceFromEntityAssembler.toResourceFromEntity(linked APP window) -> carries technician and order (AAA)")
    void mapsLinkedWindow() {
      // Arrange & Act
      var resource = MaintenanceWindowResourceFromEntityAssembler.toResourceFromEntity(persistedAppWindow());

      // Assert
      assertThat(resource.origin()).isEqualTo("APP");
      assertThat(resource.technicianId()).isEqualTo(TECHNICIAN_PROFILE_ID);
      assertThat(resource.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
      assertThat(resource.unlinked()).isFalse();
    }

    @Test
    @DisplayName("MaintenanceWindowResourceFromEntityAssembler.toResourceFromEntity(closed window) -> inactive with its reason (AAA)")
    void mapsClosedWindow() {
      // Arrange & Act
      var resource = MaintenanceWindowResourceFromEntityAssembler.toResourceFromEntity(closedWindow());

      // Assert
      assertThat(resource.endedAt()).isEqualTo(T0.plusSeconds(600));
      assertThat(resource.closureReason()).isEqualTo("MANUAL_BUTTON");
      assertThat(resource.active()).isFalse();
    }
  }

  @Nested
  class ReadingAssemblers {

    @Test
    @DisplayName("SensorReadingResourceFromEntityAssembler.toResourceFromEntity(reading) -> maps measurements and environment (AAA)")
    void mapsReading() {
      // Arrange
      var reading = sensorReading();
      reading.assignId(READING_ID);

      // Act
      var resource = SensorReadingResourceFromEntityAssembler.toResourceFromEntity(reading);

      // Assert
      assertThat(resource.id()).isEqualTo(READING_ID);
      assertThat(resource.readingUid()).isEqualTo(READING_UID.toString());
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.deviceId()).isEqualTo(DEVICE_ID);
      assertThat(resource.minTemperature()).isEqualTo(4.0);
      assertThat(resource.avgTemperature()).isEqualTo(5.0);
      assertThat(resource.maxTemperature()).isEqualTo(6.0);
      assertThat(resource.humidity()).isEqualTo(55.0);
      assertThat(resource.sampleCount()).isEqualTo(40);
      assertThat(resource.lightLevelMax()).isEqualTo(10.0);
      assertThat(resource.doorOpenSeconds()).isEqualTo(12);
      assertThat(resource.presenceDetected()).isTrue();
      assertThat(resource.recordedAt()).isEqualTo(T0);
      assertThat(resource.appliedConfigVersion()).isEqualTo(1L);
      assertThat(resource.underMaintenance()).isFalse();
    }

    @Test
    @DisplayName("TelemetryBucketResourceFromValueAssembler.toResourceFromValue(bucket) -> maps every aggregate (AAA)")
    void mapsBucket() {
      // Arrange
      var bucket = new TelemetryBucket(T0, T0.plusSeconds(300), 3.0, 7.0, 5.0, 60.0, 80, 4, true, 42L, T0.plusSeconds(299));

      // Act
      var resource = TelemetryBucketResourceFromValueAssembler.toResourceFromValue(bucket);

      // Assert
      assertThat(resource.bucketStart()).isEqualTo(T0);
      assertThat(resource.bucketEnd()).isEqualTo(T0.plusSeconds(300));
      assertThat(resource.minTemperature()).isEqualTo(3.0);
      assertThat(resource.maxTemperature()).isEqualTo(7.0);
      assertThat(resource.avgTemperature()).isEqualTo(5.0);
      assertThat(resource.avgHumidity()).isEqualTo(60.0);
      assertThat(resource.sampleCount()).isEqualTo(80);
      assertThat(resource.batchCount()).isEqualTo(4);
      assertThat(resource.underMaintenance()).isTrue();
      assertThat(resource.doorOpenSeconds()).isEqualTo(42L);
      assertThat(resource.lastReceivedAt()).isEqualTo(T0.plusSeconds(299));
    }

    @Test
    @DisplayName("RecordReadingBatchCommandFromResourceAssembler.toCommandFromResource(resource, device) -> takes ids from the device (AAA)")
    void mapsReadingBatchCommand() {
      // Arrange: the body names another equipment, but the authenticated device decides
      var resource = new RecordReadingBatchResource(READING_UID, OTHER_EQUIPMENT_ID, 999L,
          4.0, 6.0, 5.0, 4.1, 6.1, 5.1, 55.0, 40, 10.0, 12, true, T0, 3L);

      // Act
      var command = RecordReadingBatchCommandFromResourceAssembler.toCommandFromResource(
          resource, new AuthenticatedDevice(DEVICE_ID, EQUIPMENT_ID));

      // Assert
      assertThat(command.readingUid()).isEqualTo(READING_UID);
      assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(command.deviceId()).isEqualTo(DEVICE_ID);
      assertThat(command.minTemperature()).isEqualTo(4.0);
      assertThat(command.maxTemperature()).isEqualTo(6.0);
      assertThat(command.avgTemperature()).isEqualTo(5.0);
      assertThat(command.rawMinTemperature()).isEqualTo(4.1);
      assertThat(command.rawMaxTemperature()).isEqualTo(6.1);
      assertThat(command.rawAvgTemperature()).isEqualTo(5.1);
      assertThat(command.humidity()).isEqualTo(55.0);
      assertThat(command.sampleCount()).isEqualTo(40);
      assertThat(command.lightLevelMax()).isEqualTo(10.0);
      assertThat(command.doorOpenSeconds()).isEqualTo(12);
      assertThat(command.presenceDetected()).isTrue();
      assertThat(command.recordedAt()).isEqualTo(T0);
      assertThat(command.appliedConfigVersion()).isEqualTo(3L);
    }
  }

  @Nested
  class EdgeEventAssemblers {

    @Test
    @DisplayName("EdgeEventAssembler.toCommandFromResource(maintenance start) -> maps enums and optional ids (AAA)")
    void mapsMaintenanceStart() {
      // Arrange
      var resource = new RecordEdgeEventResource(EVENT_UID, EdgeEventTypeResource.MAINTENANCE_WINDOW_STARTED, T0,
          null, null, null, OTHER_EQUIPMENT_ID, 999L, MaintenanceOriginResource.APP,
          TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID, null, null);

      // Act
      var command = EdgeEventAssembler.toCommandFromResource(resource, new AuthenticatedDevice(DEVICE_ID, EQUIPMENT_ID));

      // Assert
      assertThat(command.eventUid()).isEqualTo(EVENT_UID);
      assertThat(command.type()).isEqualTo(EdgeEventType.MAINTENANCE_WINDOW_STARTED);
      assertThat(command.deviceId()).isEqualTo(DEVICE_ID);
      assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(command.occurredAt()).isEqualTo(T0);
      assertThat(command.origin()).isEqualTo(MaintenanceOrigin.APP);
      assertThat(command.technicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
      assertThat(command.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
      assertThat(command.closureReason()).isNull();
    }

    @Test
    @DisplayName("EdgeEventAssembler.toCommandFromResource(excursion detected) -> maps temperatures and reading, no window data (AAA)")
    void mapsExcursionDetected() {
      // Arrange
      var resource = new RecordEdgeEventResource(EVENT_UID, EdgeEventTypeResource.THERMAL_EXCURSION_DETECTED, T0,
          TEMPERATURE, 9.0, READING_UID, null, null, null, null, null, null, null);

      // Act
      var command = EdgeEventAssembler.toCommandFromResource(resource, new AuthenticatedDevice(DEVICE_ID, EQUIPMENT_ID));

      // Assert
      assertThat(command.type()).isEqualTo(EdgeEventType.THERMAL_EXCURSION_DETECTED);
      assertThat(command.temperatureCelsius()).isEqualTo(TEMPERATURE);
      assertThat(command.peakTemperatureCelsius()).isEqualTo(9.0);
      assertThat(command.readingUid()).isEqualTo(READING_UID);
      assertThat(command.origin()).isNull();
      assertThat(command.closureReason()).isNull();
    }

    @Test
    @DisplayName("EdgeEventAssembler.toCommandFromResource(maintenance end) -> maps closure reason and start uid (AAA)")
    void mapsMaintenanceEnd() {
      // Arrange
      var resource = new RecordEdgeEventResource(EVENT_UID, EdgeEventTypeResource.MAINTENANCE_WINDOW_ENDED, T0,
          null, null, null, null, null, null, null, null, ClosureReasonResource.INTERVENTION_COMPLETED, START_EVENT_UID);

      // Act
      var command = EdgeEventAssembler.toCommandFromResource(resource, new AuthenticatedDevice(DEVICE_ID, EQUIPMENT_ID));

      // Assert
      assertThat(command.closureReason()).isEqualTo(ClosureReason.INTERVENTION_COMPLETED);
      assertThat(command.startEventUid()).isEqualTo(START_EVENT_UID);
    }

    @Test
    @DisplayName("EdgeEventAssembler.toResourceFromEntity(event) -> maps type and result names (AAA)")
    void mapsEdgeEventResource() {
      // Arrange
      var event = edgeEvent(EdgeEventType.THERMAL_EXCURSION_DETECTED, EdgeEventResult.ALERT_OPENED);

      // Act
      var resource = EdgeEventAssembler.toResourceFromEntity(event);

      // Assert
      assertThat(resource.eventUid()).isEqualTo(EVENT_UID);
      assertThat(resource.type()).isEqualTo("THERMAL_EXCURSION_DETECTED");
      assertThat(resource.occurredAt()).isEqualTo(T0);
      assertThat(resource.result()).isEqualTo("ALERT_OPENED");
      assertThat(resource.alertId()).isNull();
      assertThat(resource.maintenanceWindowId()).isNull();
    }
  }

  @Nested
  class StreamAssemblers {

    @Test
    @DisplayName("StreamTicketResourceFromResultAssembler.toResourceFromResult(ticket) -> expiry in seconds (AAA)")
    void mapsStreamTicket() {
      // Arrange
      var issued = new IssuedStreamTicket("st_abc", IssuedStreamTicket.TTL);

      // Act
      var resource = StreamTicketResourceFromResultAssembler.toResourceFromResult(issued);

      // Assert
      assertThat(resource.ticket()).isEqualTo("st_abc");
      assertThat(resource.expiresIn()).isEqualTo(60L);
    }

    @Test
    @DisplayName("TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(ReadingRecordedEvent) -> READING_RECORDED payload (AAA)")
    void mapsReadingRecordedEvent() {
      // Arrange
      var event = new ReadingRecordedEvent(READING_UID, EQUIPMENT_ID, DEVICE_ID, 4.0, 6.0, 5.0, 55.0, 40,
          T0, T0.plusSeconds(2), true);

      // Act
      var resource = TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(event);

      // Assert
      assertThat(resource.type()).isEqualTo(TelemetryStreamResourceFromEventAssembler.READING_RECORDED);
      assertThat(resource.readingUid()).isEqualTo(READING_UID.toString());
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.deviceId()).isEqualTo(DEVICE_ID);
      assertThat(resource.minTemperature()).isEqualTo(4.0);
      assertThat(resource.maxTemperature()).isEqualTo(6.0);
      assertThat(resource.avgTemperature()).isEqualTo(5.0);
      assertThat(resource.humidity()).isEqualTo(55.0);
      assertThat(resource.sampleCount()).isEqualTo(40);
      assertThat(resource.recordedAt()).isEqualTo(T0);
      assertThat(resource.receivedAt()).isEqualTo(T0.plusSeconds(2));
      assertThat(resource.underMaintenance()).isTrue();
    }

    @Test
    @DisplayName("TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(ReadingRecordedEvent without uid) -> null uid (AAA)")
    void mapsReadingWithoutUid() {
      // Arrange
      var event = new ReadingRecordedEvent(null, EQUIPMENT_ID, DEVICE_ID, 4.0, 6.0, 5.0, 55.0, 40, T0, T0, false);

      // Act
      var resource = TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(event);

      // Assert
      assertThat(resource.readingUid()).isNull();
      assertThat(resource.underMaintenance()).isFalse();
    }

    @Test
    @DisplayName("TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(AlertStatusChangedEvent) -> ALERT_STATUS_CHANGED payload (AAA)")
    void mapsAlertStatusChangedEvent() {
      // Arrange
      var event = new AlertStatusChangedEvent(ALERT_ID, EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION,
          AlertSeverity.CRITICAL, AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED, TEMPERATURE, T0);

      // Act
      var resource = TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(event);

      // Assert
      assertThat(resource.type()).isEqualTo(TelemetryStreamResourceFromEventAssembler.ALERT_STATUS_CHANGED);
      assertThat(resource.alertId()).isEqualTo(ALERT_ID);
      assertThat(resource.equipmentId()).isEqualTo(EQUIPMENT_ID);
      assertThat(resource.alertType()).isEqualTo("TEMPERATURE_EXCURSION");
      assertThat(resource.severity()).isEqualTo("CRITICAL");
      assertThat(resource.previousStatus()).isEqualTo("OPEN");
      assertThat(resource.status()).isEqualTo("ACKNOWLEDGED");
      assertThat(resource.peakTemperature()).isEqualTo(TEMPERATURE);
      assertThat(resource.changedAt()).isEqualTo(T0);
    }

    @Test
    @DisplayName("TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(newly raised alert) -> null previous status (AAA)")
    void mapsNewlyRaisedAlert() {
      // Arrange
      var event = new AlertStatusChangedEvent(ALERT_ID, EQUIPMENT_ID, null, null, null, AlertStatus.OPEN, null, T0);

      // Act
      var resource = TelemetryStreamResourceFromEventAssembler.toResourceFromEvent(event);

      // Assert
      assertThat(resource.alertType()).isNull();
      assertThat(resource.severity()).isNull();
      assertThat(resource.previousStatus()).isNull();
      assertThat(resource.status()).isEqualTo("OPEN");
    }
  }
}
