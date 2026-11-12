package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalDeviceServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.results.ReadingIngestionOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.recordReadingBatchCommand;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class SensorReadingCommandServiceImplTest {

  private static final double[] THRESHOLD = {2.0, 8.0};
  private static final Instant LATER = T0.plus(Duration.ofMinutes(10));

  @Mock
  private SensorReadingRepository sensorReadingRepository;
  @Mock
  private AlertRepository alertRepository;
  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private ExternalAssetManagementService externalAssetManagementService;
  @Mock
  private ExternalDeviceServiceForMonitoring externalDeviceService;
  @Mock
  private MaintenanceWindowRepository maintenanceWindowRepository;
  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private SensorReadingCommandServiceImpl service;

  private void givenNewReadingIsPersisted() {
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.empty());
    lenient().when(sensorReadingRepository.save(any(SensorReading.class))).thenAnswer(invocation -> {
      SensorReading reading = invocation.getArgument(0);
      reading.assignId(READING_ID);
      return reading;
    });
  }

  private void givenNoOpenOfflineAlert() {
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.DEVICE_OFFLINE))
        .thenReturn(Optional.empty());
  }

  /** Second and later stubs of the same method use doReturn to avoid Mockito's strict-stubs argument check. */
  private void givenOpenExcursion(Alert alert) {
    doReturn(Optional.of(alert)).when(alertRepository)
        .findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION);
  }

  private static List<SensorReading> stableWindow(int size) {
    var readings = new ArrayList<SensorReading>();
    for (var i = 0; i < size; i++) {
      readings.add(sensorReading(LATER.minusSeconds(60L * i), 4.0, 5.0, 6.0, 40, false));
    }
    return readings;
  }


  @Test
  @DisplayName("handle(known readingUid) -> Duplicate without touching anything else (AAA)")
  void handleReturnsDuplicateForKnownReading() {
    // Arrange
    var stored = sensorReading();
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.of(stored));

    // Act
    var outcome = service.handle(recordReadingBatchCommand());

    // Assert
    assertThat(outcome).isInstanceOf(ReadingIngestionOutcome.Duplicate.class);
    assertThat(outcome.reading()).isSameAs(stored);
    verify(sensorReadingRepository, never()).save(any());
    verifyNoInteractions(transactionManager, maintenanceWindowRepository, alertRepository, alertPolicyRepository,
        externalAssetManagementService, externalDeviceService);
  }

  @Test
  @DisplayName("handle(new reading) -> stores it and refreshes the equipment and device connectivity (AAA)")
  void handleRecordsReadingAndRefreshesConnectivity() {
    // Arrange
    givenNewReadingIsPersisted();
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    givenNoOpenOfflineAlert();
    doReturn(Optional.empty()).when(alertRepository)
        .findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION);

    // Act
    var outcome = service.handle(recordReadingBatchCommand());

    // Assert
    assertThat(outcome).isInstanceOf(ReadingIngestionOutcome.Recorded.class);
    var reading = outcome.reading();
    assertThat(reading.getId()).isEqualTo(READING_ID);
    assertThat(reading.getReadingUid()).isEqualTo(READING_UID);
    assertThat(reading.getDeviceId()).isEqualTo(DEVICE_ID);
    assertThat(reading.isUnderMaintenance()).isFalse();
    verify(sensorReadingRepository, times(1)).save(reading);
    verify(externalAssetManagementService, times(1))
        .recordEquipmentReading(EQUIPMENT_ID, 5.0, T0, reading.getReceivedAt());
    verify(externalDeviceService, times(1)).recordDeviceActivity(DEVICE_ID, reading.getReceivedAt());
    verify(externalAssetManagementService, never()).fetchTemperatureThreshold(any());
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(reading inside a maintenance window) -> flags it and skips the threshold evaluation (AAA)")
  void handleFlagsReadingUnderMaintenance() {
    // Arrange
    givenNewReadingIsPersisted();
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.of(persistedButtonWindow()));
    givenNoOpenOfflineAlert();

    // Act
    var outcome = service.handle(recordReadingBatchCommand());

    // Assert
    assertThat(outcome).isInstanceOf(ReadingIngestionOutcome.Recorded.class);
    assertThat(outcome.reading().isUnderMaintenance()).isTrue();
    verify(alertRepository, never())
        .findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION);
    verify(externalAssetManagementService, times(1))
        .recordEquipmentReading(EQUIPMENT_ID, 5.0, T0, outcome.reading().getReceivedAt());
  }

  @Test
  @DisplayName("handle(reading while the unit is offline) -> resolves the DEVICE_OFFLINE alert automatically (AAA)")
  void handleResolvesOfflineAlert() {
    // Arrange
    givenNewReadingIsPersisted();
    var offline = persistedOfflineAlert();
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.DEVICE_OFFLINE))
        .thenReturn(Optional.of(offline));
    doReturn(Optional.empty()).when(alertRepository)
        .findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION);

    // Act
    service.handle(recordReadingBatchCommand());

    // Assert
    assertThat(offline.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    verify(alertRepository, times(1)).save(offline);
  }


  @Test
  @DisplayName("handle(concurrent insert) -> Duplicate of the stored reading after a constraint violation (AAA)")
  void handleReturnsDuplicateOnConcurrentInsert() {
    // Arrange
    var stored = sensorReading();
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.empty(), Optional.of(stored));
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(sensorReadingRepository.save(any(SensorReading.class)))
        .thenThrow(new DataIntegrityViolationException("uk_reading_uid"));

    // Act
    var outcome = service.handle(recordReadingBatchCommand());

    // Assert
    assertThat(outcome).isInstanceOf(ReadingIngestionOutcome.Duplicate.class);
    assertThat(outcome.reading()).isSameAs(stored);
    verify(transactionManager, times(1)).rollback(any());
    verifyNoInteractions(externalAssetManagementService, externalDeviceService);
  }

  @Test
  @DisplayName("handle(constraint violation, reading not found) -> rethrows the violation (AAA)")
  void handleRethrowsWhenViolationIsNotADuplicate() {
    // Arrange
    var violation = new DataIntegrityViolationException("other constraint");
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.empty(), Optional.empty());
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(sensorReadingRepository.save(any(SensorReading.class))).thenThrow(violation);

    // Act & Assert
    assertThatThrownBy(() -> service.handle(recordReadingBatchCommand())).isSameAs(violation);
  }


  @Test
  @DisplayName("handle(reading, no open excursion) -> never asks for the threshold (AAA)")
  void handleWithoutOpenExcursionSkipsEvaluation() {
    // Arrange
    givenNewReadingIsPersisted();
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    givenNoOpenOfflineAlert();
    doReturn(Optional.empty()).when(alertRepository)
        .findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION);

    // Act
    service.handle(recordReadingBatchCommand());

    // Assert
    verifyNoInteractions(alertPolicyRepository);
    verify(externalAssetManagementService, never()).fetchTemperatureThreshold(any());
  }

  @Test
  @DisplayName("handle(open excursion, unknown threshold) -> leaves the alert open (AAA)")
  void handleWithoutThresholdKeepsAlertOpen() {
    // Arrange
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0.plus(Duration.ofMinutes(10))))
        .thenReturn(Optional.empty());
    givenNoOpenOfflineAlert();
    givenOpenExcursion(alert);
    when(externalAssetManagementService.fetchTemperatureThreshold(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    verify(alertRepository, never()).save(any());
    verifyNoInteractions(alertPolicyRepository);
  }

  @Test
  @DisplayName("handle(stable readings for the sustained window) -> resolves the excursion automatically (AAA)")
  void handleResolvesExcursionWhenRecovered() {
    // Arrange
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    givenRecoveryContext(alert, equipmentPolicy(), stableWindow(5));

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    verify(alertRepository, times(1)).save(alert);
  }

  @Test
  @DisplayName("handle(no stored policy) -> falls back to the platform default policy (AAA)")
  void handleUsesPlatformDefaultWhenNoPolicy() {
    // Arrange
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    givenRecoveryContext(alert, null, stableWindow(AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES));

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    verify(sensorReadingRepository, times(1)).findByEquipmentIdAndRecordedAtBetween(
        EQUIPMENT_ID, LATER.minus(Duration.ofMinutes(AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES)), LATER);
  }

  @Test
  @DisplayName("handle(fewer readings than the sustained minutes) -> keeps the alert open (AAA)")
  void handleKeepsAlertOpenWhenWindowTooSparse() {
    // Arrange
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    givenRecoveryContext(alert, equipmentPolicy(), stableWindow(4));

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(readings under maintenance in the window) -> they do not count towards recovery (AAA)")
  void handleIgnoresMaintenanceReadingsInWindow() {
    // Arrange
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    var window = stableWindow(4);
    window.add(sensorReading(LATER.minusSeconds(600), 4.0, 5.0, 6.0, 40, true));
    givenRecoveryContext(alert, equipmentPolicy(), window);

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
  }

  @Test
  @DisplayName("handle(upper bound within the hysteresis margin) -> keeps the alert open (AAA)")
  void handleKeepsAlertOpenInsideHysteresisMargin() {
    // Arrange: threshold max 8.0 minus margin 1.5 = 6.5; one reading peaks at 7.0
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    var window = stableWindow(4);
    window.add(sensorReading(LATER.minusSeconds(600), 4.0, 5.0, 7.0, 40, false));
    givenRecoveryContext(alert, equipmentPolicy(), window);

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(window starting before the excursion began) -> does not even query the readings (AAA)")
  void handleKeepsAlertOpenWhenWindowOverlapsExcursionStart() {
    // Arrange: a 5-minute window ending at T0+5min starts exactly at the excursion
    givenNewReadingIsPersisted();
    var alert = persistedTemperatureAlert();
    var recordedAt = T0.plus(Duration.ofMinutes(5));
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, recordedAt)).thenReturn(Optional.empty());
    givenNoOpenOfflineAlert();
    givenOpenExcursion(alert);
    when(externalAssetManagementService.fetchTemperatureThreshold(EQUIPMENT_ID)).thenReturn(Optional.of(THRESHOLD));
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(equipmentPolicy()));

    // Act
    service.handle(recordReadingBatchCommand(recordedAt));

    // Assert
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    verify(sensorReadingRepository, never()).findByEquipmentIdAndRecordedAtBetween(any(), any(), any());
  }

  @Test
  @DisplayName("handle(cold excursion, readings still near the minimum) -> keeps the alert open (AAA)")
  void handleKeepsColdExcursionOpenInsideMargin() {
    // Arrange: minimum 2.0 plus margin 1.5 = 3.5; one reading bottoms at 3.0
    givenNewReadingIsPersisted();
    var cold = Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, 1.0, T0, Duration.ofMinutes(2));
    cold.assignId(ALERT_ID);
    var window = stableWindow(4);
    window.add(sensorReading(LATER.minusSeconds(600), 3.0, 5.0, 6.0, 40, false));
    givenRecoveryContext(cold, equipmentPolicy(), window);

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(cold.getStatus()).isEqualTo(AlertStatus.OPEN);
  }

  @Test
  @DisplayName("handle(cold excursion, readings back above the margin) -> resolves the alert (AAA)")
  void handleResolvesColdExcursion() {
    // Arrange
    givenNewReadingIsPersisted();
    var cold = Alert.raisedFromEdge(EQUIPMENT_ID, READING_ID, 1.0, T0, Duration.ofMinutes(2));
    cold.assignId(ALERT_ID);
    givenRecoveryContext(cold, equipmentPolicy(), stableWindow(5));

    // Act
    service.handle(recordReadingBatchCommand(LATER));

    // Assert
    assertThat(cold.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    var captor = ArgumentCaptor.forClass(Alert.class);
    verify(alertRepository, times(1)).save(captor.capture());
    assertThat(captor.getValue()).isSameAs(cold);
  }

  private void givenRecoveryContext(Alert alert, AlertPolicy policy, List<SensorReading> window) {
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, LATER)).thenReturn(Optional.empty());
    givenNoOpenOfflineAlert();
    givenOpenExcursion(alert);
    when(externalAssetManagementService.fetchTemperatureThreshold(EQUIPMENT_ID)).thenReturn(Optional.of(THRESHOLD));
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.ofNullable(policy));
    var sustained = policy == null ? AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES
        : policy.getSustainedExcursionMinutes();
    when(sensorReadingRepository.findByEquipmentIdAndRecordedAtBetween(
        EQUIPMENT_ID, LATER.minus(Duration.ofMinutes(sustained)), LATER)).thenReturn(window);
  }
}
