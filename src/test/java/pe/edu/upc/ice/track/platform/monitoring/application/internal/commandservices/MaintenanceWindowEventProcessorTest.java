package pe.edu.upc.ice.track.platform.monitoring.application.internal.commandservices;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalServiceRequestServiceForMonitoring;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ServiceRequestAttribution;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.MaintenanceWindow;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.RecordEdgeEventCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowOpenedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.MaintenanceWindowPromotedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventResult;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.EdgeEventType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.MaintenanceWindowRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.maintenanceEndedCommand;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringCommandFixtures.maintenanceStartedCommand;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowEventProcessorTest {

  @Mock
  private MaintenanceWindowRepository maintenanceWindowRepository;
  @Mock
  private ExternalServiceRequestServiceForMonitoring externalServiceRequestService;

  @InjectMocks
  private MaintenanceWindowEventProcessor processor;

  private void givenSaveAssignsIds() {
    lenient().when(maintenanceWindowRepository.save(any(MaintenanceWindow.class))).thenAnswer(invocation -> {
      MaintenanceWindow window = invocation.getArgument(0);
      if (window.getId() == null) {
        window.assignId(WINDOW_ID);
      }
      return window;
    });
  }

  private static RecordEdgeEventCommand startAt(Instant occurredAt, MaintenanceOrigin origin,
                                                Long technicianId, Long serviceRequestId) {
    return new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.MAINTENANCE_WINDOW_STARTED, DEVICE_ID,
        EQUIPMENT_ID, occurredAt, null, null, null, origin, technicianId, serviceRequestId, null, null);
  }

  private static RecordEdgeEventCommand endWithoutStartUid(ClosureReason reason) {
    return new RecordEdgeEventCommand(EVENT_UID, EdgeEventType.MAINTENANCE_WINDOW_ENDED, DEVICE_ID,
        EQUIPMENT_ID, T0.plusSeconds(600), null, null, null, null, null, null, reason, null);
  }


  @Test
  @DisplayName("start(APP command with order) -> opens a window without consulting the ACL (AAA)")
  void startOpensWindowWithExplicitAttribution() {
    // Arrange
    givenSaveAssignsIds();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());
    var command = maintenanceStartedCommand(MaintenanceOrigin.APP);

    // Act
    var outcome = processor.start(command);

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_OPENED);
    assertThat(outcome.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    var captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
    verify(maintenanceWindowRepository, times(2)).save(captor.capture());
    var window = captor.getAllValues().get(0);
    assertThat(window.getOrigin()).isEqualTo(MaintenanceOrigin.APP);
    assertThat(window.getStartEventUid()).isEqualTo(EVENT_UID);
    assertThat(window.getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
    assertThat(window.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(window.domainEvents()).hasAtLeastOneElementOfType(MaintenanceWindowOpenedEvent.class);
    verifyNoInteractions(externalServiceRequestService);
  }

  @Test
  @DisplayName("start(BUTTON command without order) -> attributes the in-progress order and its technician (AAA)")
  void startAttributesInProgressServiceRequest() {
    // Arrange
    givenSaveAssignsIds();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(externalServiceRequestService.findInProgressServiceRequestAt(EQUIPMENT_ID, T0))
        .thenReturn(Optional.of(new ServiceRequestAttribution(SERVICE_REQUEST_ID, TECHNICIAN_PROFILE_ID)));

    // Act
    var outcome = processor.start(startAt(T0, MaintenanceOrigin.BUTTON, null, null));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_OPENED);
    var captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
    verify(maintenanceWindowRepository, times(2)).save(captor.capture());
    var window = captor.getAllValues().get(0);
    assertThat(window.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(window.getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
    assertThat(window.isUnlinked()).isFalse();
  }

  @Test
  @DisplayName("start(technician sent, order missing) -> keeps the sent technician and takes the order from the ACL (AAA)")
  void startKeepsSentTechnician() {
    // Arrange
    givenSaveAssignsIds();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(externalServiceRequestService.findInProgressServiceRequestAt(EQUIPMENT_ID, T0))
        .thenReturn(Optional.of(new ServiceRequestAttribution(SERVICE_REQUEST_ID, 999L)));

    // Act
    processor.start(startAt(T0, MaintenanceOrigin.BUTTON, TECHNICIAN_PROFILE_ID, null));

    // Assert
    var captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
    verify(maintenanceWindowRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues().get(0).getTechnicianProfileId()).isEqualTo(TECHNICIAN_PROFILE_ID);
    assertThat(captor.getAllValues().get(0).getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("start(no order anywhere) -> opens an unlinked window (AAA)")
  void startOpensUnlinkedWindow() {
    // Arrange
    givenSaveAssignsIds();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());
    when(externalServiceRequestService.findInProgressServiceRequestAt(EQUIPMENT_ID, T0))
        .thenReturn(Optional.empty());

    // Act
    var outcome = processor.start(startAt(T0, MaintenanceOrigin.BUTTON, null, null));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_OPENED);
    var captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
    verify(maintenanceWindowRepository, times(2)).save(captor.capture());
    assertThat(captor.getAllValues().get(0).isUnlinked()).isTrue();
    assertThat(captor.getAllValues().get(0).getTechnicianProfileId()).isNull();
  }


  @Test
  @DisplayName("start(APP command over a BUTTON window) -> promotes it to APP (AAA)")
  void startPromotesButtonWindow() {
    // Arrange
    givenSaveAssignsIds();
    var active = persistedButtonWindow();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(active));

    // Act
    var outcome = processor.start(maintenanceStartedCommand(MaintenanceOrigin.APP));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_PROMOTED);
    assertThat(outcome.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(active.getOrigin()).isEqualTo(MaintenanceOrigin.APP);
    assertThat(active.getExpiresAt()).isEqualTo(MaintenanceOrigin.APP.expiryFrom(T0));
    assertThat(active.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(active.domainEvents()).hasAtLeastOneElementOfType(MaintenanceWindowPromotedEvent.class);
    verify(maintenanceWindowRepository, times(1)).save(active);
  }

  @Test
  @DisplayName("start(BUTTON command over a BUTTON window) -> keeps it without saving (AAA)")
  void startKeepsActiveButtonWindow() {
    // Arrange
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID))
        .thenReturn(Optional.of(persistedButtonWindow()));

    // Act
    var outcome = processor.start(maintenanceStartedCommand(MaintenanceOrigin.BUTTON));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_ALREADY_ACTIVE);
    assertThat(outcome.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    verify(maintenanceWindowRepository, never()).save(any());
    verifyNoInteractions(externalServiceRequestService);
  }

  @Test
  @DisplayName("start(APP command over an APP window) -> cannot promote, keeps it (AAA)")
  void startKeepsActiveAppWindow() {
    // Arrange
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID))
        .thenReturn(Optional.of(persistedAppWindow()));

    // Act
    var outcome = processor.start(maintenanceStartedCommand(MaintenanceOrigin.APP));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_ALREADY_ACTIVE);
    verify(maintenanceWindowRepository, never()).save(any());
  }

  @Test
  @DisplayName("start(window expired before the event) -> closes it as EXPIRED and opens a new one (AAA)")
  void startReplacesOverdueWindow() {
    // Arrange
    givenSaveAssignsIds();
    var overdue = persistedButtonWindow();
    var later = overdue.getExpiresAt().plusSeconds(1);
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(overdue));
    when(externalServiceRequestService.findInProgressServiceRequestAt(EQUIPMENT_ID, later))
        .thenReturn(Optional.empty());

    // Act
    var outcome = processor.start(startAt(later, MaintenanceOrigin.BUTTON, null, null));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_OPENED);
    assertThat(overdue.getClosureReason()).isEqualTo(ClosureReason.EXPIRED);
    assertThat(overdue.getEndedAt()).isEqualTo(overdue.getExpiresAt());
    var captor = ArgumentCaptor.forClass(MaintenanceWindow.class);
    verify(maintenanceWindowRepository, times(3)).save(captor.capture());
    assertThat(captor.getAllValues().get(0)).isSameAs(overdue);
    assertThat(captor.getAllValues().get(1)).isNotSameAs(overdue);
    assertThat(captor.getAllValues().get(1).getStartedAt()).isEqualTo(later);
  }

  @Test
  @DisplayName("start(event exactly at expiry) -> window is not overdue yet, so it is kept (AAA)")
  void startAtExactExpiryKeepsWindow() {
    // Arrange
    var active = persistedButtonWindow();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(active));

    // Act
    var outcome = processor.start(startAt(active.getExpiresAt(), MaintenanceOrigin.BUTTON, null, null));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_ALREADY_ACTIVE);
    assertThat(active.getEndedAt()).isNull();
    verify(maintenanceWindowRepository, never()).save(any());
  }

  @Test
  @DisplayName("end(startEventUid of this equipment) -> closes that window (AAA)")
  void endClosesWindowByStartEventUid() {
    // Arrange
    var window = persistedButtonWindow();
    when(maintenanceWindowRepository.findByStartEventUid(START_EVENT_UID)).thenReturn(Optional.of(window));

    // Act
    var outcome = processor.end(maintenanceEndedCommand(ClosureReason.MANUAL_BUTTON));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_CLOSED);
    assertThat(outcome.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(window.getClosureReason()).isEqualTo(ClosureReason.MANUAL_BUTTON);
    assertThat(window.getEndedAt()).isEqualTo(T0.plusSeconds(600));
    verify(maintenanceWindowRepository, times(1)).save(window);
    verify(maintenanceWindowRepository, never()).findActiveByEquipmentId(any());
  }

  @Test
  @DisplayName("end(startEventUid of another equipment) -> NO_ACTIVE_MAINTENANCE without closing (AAA)")
  void endIgnoresWindowOfAnotherEquipment() {
    // Arrange
    var foreign = new MaintenanceWindow(WINDOW_ID, START_EVENT_UID, OTHER_EQUIPMENT_ID, MaintenanceOrigin.BUTTON,
        T0, T0.plus(Duration.ofMinutes(30)), null, null, null, null);
    when(maintenanceWindowRepository.findByStartEventUid(START_EVENT_UID)).thenReturn(Optional.of(foreign));

    // Act
    var outcome = processor.end(maintenanceEndedCommand(ClosureReason.MANUAL_BUTTON));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.NO_ACTIVE_MAINTENANCE);
    assertThat(outcome.maintenanceWindowId()).isNull();
    assertThat(foreign.isActive()).isTrue();
    verify(maintenanceWindowRepository, never()).save(any());
  }

  @Test
  @DisplayName("end(unknown startEventUid) -> NO_ACTIVE_MAINTENANCE (AAA)")
  void endWithUnknownStartEventUid() {
    // Arrange
    when(maintenanceWindowRepository.findByStartEventUid(START_EVENT_UID)).thenReturn(Optional.empty());

    // Act
    var outcome = processor.end(maintenanceEndedCommand(ClosureReason.MANUAL_BUTTON));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.NO_ACTIVE_MAINTENANCE);
    assertThat(outcome.maintenanceWindowId()).isNull();
    verify(maintenanceWindowRepository, never()).save(any());
  }

  @Test
  @DisplayName("end(no startEventUid) -> closes the equipment's active window (AAA)")
  void endClosesActiveWindowWhenNoStartEventUid() {
    // Arrange
    var window = persistedButtonWindow();
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.of(window));

    // Act
    var outcome = processor.end(endWithoutStartUid(ClosureReason.INTERVENTION_COMPLETED));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.MAINTENANCE_WINDOW_CLOSED);
    assertThat(window.getClosureReason()).isEqualTo(ClosureReason.INTERVENTION_COMPLETED);
    verify(maintenanceWindowRepository, never()).findByStartEventUid(any());
    verify(maintenanceWindowRepository, times(1)).save(window);
  }

  @Test
  @DisplayName("end(no startEventUid, no active window) -> NO_ACTIVE_MAINTENANCE (AAA)")
  void endWithoutActiveWindow() {
    // Arrange
    when(maintenanceWindowRepository.findActiveByEquipmentId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    var outcome = processor.end(endWithoutStartUid(ClosureReason.MANUAL_APP));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.NO_ACTIVE_MAINTENANCE);
    assertThat(outcome.maintenanceWindowId()).isNull();
  }

  @Test
  @DisplayName("end(window already closed) -> NO_ACTIVE_MAINTENANCE keeping the window id, no save (AAA)")
  void endOnAlreadyClosedWindow() {
    // Arrange
    when(maintenanceWindowRepository.findByStartEventUid(START_EVENT_UID)).thenReturn(Optional.of(closedWindow()));

    // Act
    var outcome = processor.end(maintenanceEndedCommand(ClosureReason.MANUAL_BUTTON));

    // Assert
    assertThat(outcome.result()).isEqualTo(EdgeEventResult.NO_ACTIVE_MAINTENANCE);
    assertThat(outcome.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    verify(maintenanceWindowRepository, never()).save(any());
  }

  @Test
  @DisplayName("findCovering(equipmentId, instant) -> delegates to the repository (AAA)")
  void findCoveringDelegates() {
    // Arrange
    var window = persistedButtonWindow();
    when(maintenanceWindowRepository.findCovering(EQUIPMENT_ID, T0)).thenReturn(Optional.of(window));

    // Act
    var result = processor.findCovering(EQUIPMENT_ID, T0);

    // Assert
    assertThat(result).containsSame(window);
    verify(maintenanceWindowRepository, times(1)).findCovering(EQUIPMENT_ID, T0);
  }


  @Test
  @DisplayName("closeExpired(now, grace) -> closes the overdue windows at their expiry and counts them (AAA)")
  void closeExpiredClosesOverdueWindows() {
    // Arrange
    var now = T0.plus(Duration.ofHours(2));
    var grace = Duration.ofMinutes(30);
    var overdue = persistedButtonWindow();
    var alreadyClosed = closedWindow();
    when(maintenanceWindowRepository.findActiveExpiredBefore(now.minus(grace)))
        .thenReturn(List.of(overdue, alreadyClosed));

    // Act
    var closed = processor.closeExpired(now, grace);

    // Assert
    assertThat(closed).isEqualTo(1);
    assertThat(overdue.getClosureReason()).isEqualTo(ClosureReason.EXPIRED);
    assertThat(overdue.getEndedAt()).isEqualTo(overdue.getExpiresAt());
    verify(maintenanceWindowRepository, times(1)).save(overdue);
    verify(maintenanceWindowRepository, never()).save(alreadyClosed);
  }

  @Test
  @DisplayName("closeExpired(now, grace) with nothing overdue -> returns 0 and saves nothing (AAA)")
  void closeExpiredWithNothingOverdue() {
    // Arrange
    var now = T0.plus(Duration.ofHours(2));
    when(maintenanceWindowRepository.findActiveExpiredBefore(now.minus(Duration.ZERO))).thenReturn(List.of());

    // Act
    var closed = processor.closeExpired(now, Duration.ZERO);

    // Assert
    assertThat(closed).isZero();
    verify(maintenanceWindowRepository, never()).save(any());
  }
}
