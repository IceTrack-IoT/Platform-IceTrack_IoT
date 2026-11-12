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
import pe.edu.upc.ice.track.platform.monitoring.application.results.EdgeEventOutcome;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.EdgeEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.SensorReading;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordEdgeEventCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.EdgeEventRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.SensorReadingRepository;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class EdgeEventCommandServiceImplTest {

  @Mock
  private EdgeEventRepository edgeEventRepository;
  @Mock
  private AlertRepository alertRepository;
  @Mock
  private AlertPolicyRepository alertPolicyRepository;
  @Mock
  private SensorReadingRepository sensorReadingRepository;
  @Mock
  private MaintenanceWindowEventProcessor maintenanceWindowEventProcessor;
  @Mock
  private PlatformTransactionManager transactionManager;

  @InjectMocks
  private EdgeEventCommandServiceImpl service;

  private void givenRepositoriesPersist() {
    lenient().when(edgeEventRepository.save(any(EdgeEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> {
      Alert alert = invocation.getArgument(0);
      if (alert.getId() == null) {
        alert.assignId(ALERT_ID);
      }
      return alert;
    });
  }

  private EdgeEvent savedEdgeEvent() {
    var captor = ArgumentCaptor.forClass(EdgeEvent.class);
    verify(edgeEventRepository, times(1)).save(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("handle(known eventUid) -> Duplicate without opening a transaction (AAA)")
  void handleReturnsDuplicateForKnownEvent() {
    // Arrange
    var stored = edgeEvent(EdgeEventType.THERMAL_EXCURSION_DETECTED, EdgeEventResult.ALERT_OPENED);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.of(stored));

    // Act
    var outcome = service.handle(excursionDetectedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Duplicate.class);
    assertThat(outcome.event()).isSameAs(stored);
    verify(edgeEventRepository, never()).save(any());
    verifyNoInteractions(transactionManager, alertRepository, alertPolicyRepository, sensorReadingRepository,
        maintenanceWindowEventProcessor);
  }

  @Test
  @DisplayName("handle(concurrent insert) -> Duplicate of the stored event after a constraint violation (AAA)")
  void handleReturnsDuplicateOnConcurrentInsert() {
    // Arrange
    var stored = edgeEvent(EdgeEventType.THERMAL_EXCURSION_NORMALIZED, EdgeEventResult.NO_ACTIVE_ALERT);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty(), Optional.of(stored));
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());
    when(edgeEventRepository.save(any(EdgeEvent.class))).thenThrow(new DataIntegrityViolationException("uk_event_uid"));

    // Act
    var outcome = service.handle(excursionNormalizedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Duplicate.class);
    assertThat(outcome.event()).isSameAs(stored);
    verify(transactionManager, times(1)).rollback(any());
  }

  @Test
  @DisplayName("handle(constraint violation, event not found) -> rethrows the violation (AAA)")
  void handleRethrowsWhenViolationIsNotADuplicate() {
    // Arrange
    var violation = new DataIntegrityViolationException("other constraint");
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty(), Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());
    when(edgeEventRepository.save(any(EdgeEvent.class))).thenThrow(violation);

    // Act & Assert
    assertThatThrownBy(() -> service.handle(excursionNormalizedCommand())).isSameAs(violation);
  }

  @Test
  @DisplayName("handle(DETECTED under maintenance) -> SUPPRESSED_BY_MAINTENANCE linked to the window, no alert (AAA)")
  void detectedIsSuppressedByMaintenance() {
    // Arrange
    givenRepositoriesPersist();
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.findCovering(EQUIPMENT_ID, T0))
        .thenReturn(Optional.of(persistedButtonWindow()));

    // Act
    var outcome = service.handle(excursionDetectedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    var stored = savedEdgeEvent();
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.SUPPRESSED_BY_MAINTENANCE);
    assertThat(stored.getMaintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(stored.getAlertId()).isNull();
    verifyNoInteractions(alertRepository, alertPolicyRepository, sensorReadingRepository);
  }

  @Test
  @DisplayName("handle(DETECTED with an open excursion alert) -> ALERT_ALREADY_ACTIVE referencing it (AAA)")
  void detectedWithActiveAlert() {
    // Arrange
    givenRepositoriesPersist();
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.of(persistedTemperatureAlert()));

    // Act
    var outcome = service.handle(excursionDetectedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    var stored = savedEdgeEvent();
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.ALERT_ALREADY_ACTIVE);
    assertThat(stored.getAlertId()).isEqualTo(ALERT_ID);
    verify(alertRepository, never()).save(any());
    verifyNoInteractions(alertPolicyRepository, sensorReadingRepository);
  }

  @Test
  @DisplayName("handle(DETECTED, no open alert) -> opens a CRITICAL alert linked to the triggering reading (AAA)")
  void detectedOpensAlert() {
    // Arrange
    givenRepositoriesPersist();
    var reading = sensorReading();
    reading.assignId(READING_ID);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(equipmentPolicy()));
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.of(reading));

    // Act
    var outcome = service.handle(excursionDetectedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    var alertCaptor = ArgumentCaptor.forClass(Alert.class);
    verify(alertRepository, times(2)).save(alertCaptor.capture());
    var alert = alertCaptor.getAllValues().get(0);
    assertThat(alert.getType()).isEqualTo(AlertType.TEMPERATURE_EXCURSION);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.OPEN);
    assertThat(alert.getTriggeringReadingId()).isEqualTo(READING_ID);
    assertThat(alert.getExcursionDuration()).isEqualTo(Duration.ofMinutes(equipmentPolicy().getSustainedExcursionMinutes()));
    assertThat(alert.domainEvents()).isNotEmpty();
    var stored = savedEdgeEvent();
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.ALERT_OPENED);
    assertThat(stored.getAlertId()).isEqualTo(ALERT_ID);
    assertThat(stored.getTemperatureCelsius()).isEqualTo(TEMPERATURE);
  }

  @Test
  @DisplayName("handle(DETECTED without policy) -> uses the built-in platform default duration (AAA)")
  void detectedWithoutPolicyUsesDefault() {
    // Arrange
    givenRepositoriesPersist();
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.empty());

    // Act
    service.handle(excursionDetectedCommand());

    // Assert
    var alertCaptor = ArgumentCaptor.forClass(Alert.class);
    verify(alertRepository, times(2)).save(alertCaptor.capture());
    assertThat(alertCaptor.getAllValues().get(0).getExcursionDuration())
        .isEqualTo(Duration.ofMinutes(AlertPolicy.DEFAULT_SUSTAINED_EXCURSION_MINUTES));
    assertThat(alertCaptor.getAllValues().get(0).getTriggeringReadingId()).isNull();
  }

  @Test
  @DisplayName("handle(DETECTED, reading of another equipment) -> the alert is not linked to that reading (AAA)")
  void detectedIgnoresReadingOfAnotherEquipment() {
    // Arrange
    givenRepositoriesPersist();
    var foreign = SensorReading.record(READING_UID, new EquipmentId(OTHER_EQUIPMENT_ID), DEVICE_ID,
        4.0, 6.0, 5.0, 4.0, 6.0, 5.0, 55.0, 40, new EnvironmentObservation(10.0, 12, true), T0, 1L, false);
    foreign.assignId(READING_ID);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(equipmentPolicy()));
    when(sensorReadingRepository.findByReadingUid(READING_UID)).thenReturn(Optional.of(foreign));

    // Act
    service.handle(excursionDetectedCommand());

    // Assert
    var alertCaptor = ArgumentCaptor.forClass(Alert.class);
    verify(alertRepository, times(2)).save(alertCaptor.capture());
    assertThat(alertCaptor.getAllValues().get(0).getTriggeringReadingId()).isNull();
  }

  @Test
  @DisplayName("handle(DETECTED without readingUid) -> does not look up any reading (AAA)")
  void detectedWithoutReadingUid() {
    // Arrange
    givenRepositoriesPersist();
    var command = new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.THERMAL_EXCURSION_DETECTED, DEVICE_ID,
        EQUIPMENT_ID, T0, TEMPERATURE, TEMPERATURE, null, null, null, null, null, null);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());
    when(alertPolicyRepository.findApplicableTo(EQUIPMENT_ID)).thenReturn(Optional.of(equipmentPolicy()));

    // Act
    var outcome = service.handle(command);

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    assertThat(outcome.event().getResult()).isEqualTo(EdgeEventResult.ALERT_OPENED);
    verifyNoInteractions(sensorReadingRepository);
  }

  @Test
  @DisplayName("handle(NORMALIZED with an open alert) -> resolves it from the edge, ALERT_RESOLVED (AAA)")
  void normalizedResolvesAlert() {
    // Arrange
    givenRepositoriesPersist();
    var alert = persistedTemperatureAlert();
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.of(alert));

    // Act
    var outcome = service.handle(excursionNormalizedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    assertThat(alert.getStatus()).isEqualTo(AlertStatus.RESOLVED);
    assertThat(alert.getResolvedAt()).isEqualTo(T0);
    verify(alertRepository, times(1)).save(alert);
    var stored = savedEdgeEvent();
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.ALERT_RESOLVED);
    assertThat(stored.getAlertId()).isEqualTo(ALERT_ID);
    verifyNoInteractions(maintenanceWindowEventProcessor);
  }

  @Test
  @DisplayName("handle(NORMALIZED without open alert) -> NO_ACTIVE_ALERT (AAA)")
  void normalizedWithoutAlert() {
    // Arrange
    givenRepositoriesPersist();
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(alertRepository.findOpenAlertByEquipmentIdAndType(EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION))
        .thenReturn(Optional.empty());

    // Act
    var outcome = service.handle(excursionNormalizedCommand());

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    var stored = savedEdgeEvent();
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.NO_ACTIVE_ALERT);
    assertThat(stored.getAlertId()).isNull();
    verify(alertRepository, never()).save(any());
  }

  @Test
  @DisplayName("handle(MAINTENANCE_WINDOW_STARTED) -> stores the processor's outcome (AAA)")
  void startedDelegatesToProcessor() {
    // Arrange
    givenRepositoriesPersist();
    var command = maintenanceStartedCommand(MaintenanceOrigin.APP);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.start(command)).thenReturn(
        new MaintenanceWindowEventProcessor.Outcome(EdgeEventResult.MAINTENANCE_WINDOW_OPENED, WINDOW_ID));

    // Act
    var outcome = service.handle(command);

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    var stored = savedEdgeEvent();
    assertThat(stored.getType()).isEqualTo(EdgeEventType.MAINTENANCE_WINDOW_STARTED);
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_OPENED);
    assertThat(stored.getMaintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(stored.getAlertId()).isNull();
    verifyNoInteractions(alertRepository);
  }

  @Test
  @DisplayName("handle(MAINTENANCE_WINDOW_ENDED without window) -> stores NO_ACTIVE_MAINTENANCE (AAA)")
  void endedDelegatesToProcessor() {
    // Arrange
    givenRepositoriesPersist();
    var command = maintenanceEndedCommand(ClosureReason.MANUAL_BUTTON);
    when(edgeEventRepository.findByEventUid(EVENT_UID)).thenReturn(Optional.empty());
    when(maintenanceWindowEventProcessor.end(command)).thenReturn(
        new MaintenanceWindowEventProcessor.Outcome(EdgeEventResult.NO_ACTIVE_MAINTENANCE, null));

    // Act
    var outcome = service.handle(command);

    // Assert
    assertThat(outcome).isInstanceOf(EdgeEventOutcome.Processed.class);
    var stored = savedEdgeEvent();
    assertThat(stored.getResult()).isEqualTo(EdgeEventResult.NO_ACTIVE_MAINTENANCE);
    assertThat(stored.getMaintenanceWindowId()).isNull();
    verify(maintenanceWindowEventProcessor, never()).start(any());
  }
}
