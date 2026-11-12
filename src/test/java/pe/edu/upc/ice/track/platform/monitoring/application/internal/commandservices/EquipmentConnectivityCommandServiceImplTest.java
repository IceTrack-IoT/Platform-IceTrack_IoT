package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DetectOfflineEquipmentCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EquipmentConnectivityCommandServiceImplTest {

  @Mock
  private SensorReadingRepository sensorReadingRepository;
  @Mock
  private AlertRepository alertRepository;
  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private MaintenanceWindowRepository maintenanceWindowRepository;
  @Mock
  private ExternalAssetManagementService externalAssetManagementService;
  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private EquipmentConnectivityCommandServiceImpl service;
  private SensorReading lastReading;

  private void givenOnline(Long... equipmentIds) {
    when(externalAssetManagementService.fetchOnlineEquipmentIds()).thenReturn(List.of(equipmentIds));
  }

  private void givenLastReading() {
    lastReading = sensorReading();
    when(sensorReadingRepository.findLatestReceivedByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(lastReading));
  }

  private Instant silentFor(Duration silence) {
    return lastReading.getReceivedAt().plus(silence);
  }

  private void givenSavePersists() {
    lenient().when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
      Alert alert = invocation.getArgument(0);
      if (alert.getId() == null) {
        alert.assignId(ALERT_ID);
      }
      return alert;
    });
  }

  @Test
  @DisplayName("handle(no online equipment) -> returns 0 (AAA)")
  void handleWithoutOnlineEquipment() {
    // Arrange
    givenOnline();

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(Instant.now()));

    // Assert
    assertThat(raised).isZero();
    verifyNoInteractions(sensorReadingRepository, alertRepository, transactionManager);
  }

  @Test
  @DisplayName("handle(equipment never reported) -> no alert (AAA)")
  void handleEquipmentWithoutReadings() {
    // Arrange
    givenOnline(EQUIPMENT_ID);
    when(sensorReadingRepository.findLatestReceivedByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(Instant.now()));

    // Assert
    assertThat(raised).isZero();
    verify(externalAssetManagementService, never()).markEquipmentOffline(any());
    verifyNoInteractions(alertRepository, alertPolicyRepository);
  }

  @Test
  @DisplayName("handle(silence shorter than the offline window) -> no alert (AAA)")
  void handleEquipmentStillWithinWindow() {
    // Arrange
    givenOnline(EQUIPMENT_ID);
    givenLastReading();
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act: the platform default tolerates 3 sync windows of 60 s
    var raised = service.handle(new DetectOfflineEquipmentCommand(silentFor(Duration.ofSeconds(150))));

    // Assert
    assertThat(raised).isZero();
    verify(externalAssetManagementService, never()).markEquipmentOffline(any());
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(equipment policy tolerates more windows) -> uses the equipment policy, no alert (AAA)")
  void handleUsesEquipmentPolicy() {
    // Arrange: the equipment policy tolerates 4 windows (240 s), the default would already fire at 200 s
    givenOnline(EQUIPMENT_ID);
    givenLastReading();
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(equipmentPolicy()));

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(silentFor(Duration.ofSeconds(200))));

    // Assert
    assertThat(raised).isZero();
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(silent equipment under maintenance) -> no offline alert and not marked offline (AAA)")
  void handleSilentEquipmentUnderMaintenance() {
    // Arrange
    givenOnline(EQUIPMENT_ID);
    givenLastReading();
    var now = silentFor(Duration.ofMinutes(10));
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, now)).thenReturn(Optional.of(persistedButtonWindow()));

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(now));

    // Assert
    assertThat(raised).isZero();
    verify(externalAssetManagementService, never()).markEquipmentOffline(any());
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(silent equipment, offline alert already open) -> marks it offline but raises nothing (AAA)")
  void handleSilentEquipmentWithOpenOfflineAlert() {
    // Arrange
    givenOnline(EQUIPMENT_ID);
    givenLastReading();
    var now = silentFor(Duration.ofMinutes(10));
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, now)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.DEVICE_OFFLINE))
        .thenReturn(Optional.of(persistedOfflineAlert()));

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(now));

    // Assert
    assertThat(raised).isZero();
    verify(externalAssetManagementService, times(1)).markEquipmentOffline(EQUIPMENT_ID);
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(silent equipment) -> marks it offline and raises a WARNING DEVICE_OFFLINE alert (AAA)")
  void handleRaisesOfflineAlert() {
    // Arrange
    givenOnline(EQUIPMENT_ID);
    givenLastReading();
    givenSavePersists();
    var now = silentFor(Duration.ofMinutes(10));
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, now)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.DEVICE_OFFLINE))
        .thenReturn(Optional.empty());

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(now));

    // Assert
    assertThat(raised).isEqualTo(1);
    verify(externalAssetManagementService, times(1)).markEquipmentOffline(EQUIPMENT_ID);
    var captor = ArgumentCaptor.forClass(Alert.class);
    verify(alertRepository, times(2)).save(captor.capture());
    var alert = captor.getAllValues().get(0);
    assertThat(alert.getType()).isEqualTo(AlertType.DEVICE_OFFLINE);
    assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.WARNING);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    assertThat(alert.getEquipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(alert.getTriggeringRecordedAt()).isEqualTo(lastReading.getReceivedAt());
    assertThat(alert.domainEvents()).isNotEmpty();
  }

  @Test
  @DisplayName("handle(failure on one equipment) -> logs it and keeps checking the others (AAA)")
  void handleContinuesAfterFailure() {
    // Arrange
    givenOnline(EQUIPMENT_ID, OTHER_EQUIPMENT_ID);
    givenSavePersists();
    var healthy = sensorReading();
    var now = healthy.getReceivedAt().plus(Duration.ofMinutes(10));
    when(sensorReadingRepository.findLatestReceivedByEquipmentId(EQUIPMENT_ID))
        .thenThrow(new IllegalStateException("database unavailable"));
    doReturn(Optional.of(healthy)).when(sensorReadingRepository).findLatestReceivedByEquipmentId(OTHER_EQUIPMENT_ID);
    when(alertPolicyRepository.findApplicableTo(OTHER_EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(maintenanceWindowRepository.findCovering(OTHER_EQUIPMENT_ID, now)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(OTHER_EQUIPMENT_ID, AlertType.DEVICE_OFFLINE))
        .thenReturn(Optional.empty());

    // Act
    var raised = service.handle(new DetectOfflineEquipmentCommand(now));

    // Assert
    assertThat(raised).isEqualTo(1);
    verify(transactionManager, times(1)).rollback(any());
    verify(externalAssetManagementService, times(1)).markEquipmentOffline(OTHER_EQUIPMENT_ID);
  }
}
