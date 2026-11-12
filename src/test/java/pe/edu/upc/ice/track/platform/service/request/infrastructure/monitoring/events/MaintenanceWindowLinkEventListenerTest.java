package pe.edu.upc.ice.track.platform.service.request.infrastructure.monitoring.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowClosedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowOpenedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowPromotedIntegrationEvent;
import pe.edu.upc.ice.track.platform.service.request.application.commandservices.ServiceRequestCommandService;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CompleteMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RecordMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.service.request.infrastructure.monitoring.events.MaintenanceWindowLinkEventListener;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.service.request.test.fixtures.ServiceRequestDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowLinkEventListenerTest {

  private static final Instant STARTED = Instant.parse("2026-11-12T08:00:00Z");
  private static final Instant EXPIRES = Instant.parse("2026-11-12T10:00:00Z");
  private static final Instant OTHER = Instant.parse("2026-11-12T09:00:00Z");

  @Mock private ServiceRequestCommandService commands;
  @InjectMocks private MaintenanceWindowLinkEventListener listener;

  private MaintenanceWindowOpenedIntegrationEvent opened(String origin, Long requestId) {
    return new MaintenanceWindowOpenedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, origin, STARTED, EXPIRES, TECHNICIAN_ID, requestId);
  }

  @Test
  @DisplayName("on(Opened) with an APP window -> RecordMaintenanceWindowLinkCommand attributed at the start (AAA)")
  void openedApp() {
    // Arrange
    var captor = ArgumentCaptor.forClass(RecordMaintenanceWindowLinkCommand.class);

    // Act
    listener.on(opened("APP", REQUEST_ID));

    // Assert
    verify(commands, times(1)).handle(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new RecordMaintenanceWindowLinkCommand(
        REQUEST_ID, WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, STARTED, STARTED));
  }

  @Test
  @DisplayName("on(Opened) with a BUTTON window -> origin BUTTON (AAA)")
  void openedButton() {
    // Arrange
    var captor = ArgumentCaptor.forClass(RecordMaintenanceWindowLinkCommand.class);

    // Act
    listener.on(opened("BUTTON", REQUEST_ID));

    // Assert
    verify(commands, times(1)).handle(captor.capture());
    assertThat(captor.getValue().origin()).isEqualTo(MaintenanceOrigin.BUTTON);
  }

  @Test
  @DisplayName("on(Opened) without service request -> ignored (AAA)")
  void openedUnlinked() {
    // Act
    listener.on(opened("APP", null));

    // Assert
    verifyNoInteractions(commands);
  }

  @Test
  @DisplayName("on(Opened) with an unmapped origin or a failing service -> swallowed, never propagated (AAA)")
  void openedFailures() {
    // Arrange
    doThrow(new IllegalStateException("boom")).when(commands).handle(any(RecordMaintenanceWindowLinkCommand.class));

    // Act & Assert
    assertThatCode(() -> listener.on(opened("ALIEN", REQUEST_ID))).doesNotThrowAnyException();
    assertThatCode(() -> listener.on(opened("APP", REQUEST_ID))).doesNotThrowAnyException();
    verify(commands, times(1)).handle(any(RecordMaintenanceWindowLinkCommand.class));
  }

  @Test
  @DisplayName("on(Promoted) -> link recorded with origin APP and attributed at the promotion instant (AAA)")
  void promoted() {
    // Arrange
    var captor = ArgumentCaptor.forClass(RecordMaintenanceWindowLinkCommand.class);

    // Act
    listener.on(new MaintenanceWindowPromotedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, STARTED, OTHER, EXPIRES, TECHNICIAN_ID, REQUEST_ID));

    // Assert
    verify(commands, times(1)).handle(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new RecordMaintenanceWindowLinkCommand(
        REQUEST_ID, WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.APP, TECHNICIAN_ID, STARTED, OTHER));
  }

  @Test
  @DisplayName("on(Promoted) without service request or with a failing service -> nothing recorded / swallowed (AAA)")
  void promotedEdges() {
    // Arrange
    doThrow(new IllegalStateException("boom")).when(commands).handle(any(RecordMaintenanceWindowLinkCommand.class));

    // Act & Assert
    listener.on(new MaintenanceWindowPromotedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, STARTED, OTHER, EXPIRES, TECHNICIAN_ID, null));
    verifyNoInteractions(commands);
    assertThatCode(() -> listener.on(new MaintenanceWindowPromotedIntegrationEvent(
        WINDOW_ID, EQUIPMENT_ID, STARTED, OTHER, EXPIRES, TECHNICIAN_ID, REQUEST_ID))).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("on(Closed) -> CompleteMaintenanceWindowLinkCommand with end instant and closure reason (AAA)")
  void closed() {
    // Arrange
    var captor = ArgumentCaptor.forClass(CompleteMaintenanceWindowLinkCommand.class);

    // Act
    listener.on(new MaintenanceWindowClosedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, "APP", STARTED, OTHER, "MANUAL", TECHNICIAN_ID, REQUEST_ID));

    // Assert
    verify(commands, times(1)).handle(captor.capture());
    assertThat(captor.getValue()).isEqualTo(new CompleteMaintenanceWindowLinkCommand(REQUEST_ID, WINDOW_ID, OTHER, "MANUAL"));
  }

  @Test
  @DisplayName("on(Closed) without service request -> ignored; failing service -> swallowed (AAA)")
  void closedEdges() {
    // Arrange
    doThrow(new IllegalStateException("boom")).when(commands).handle(any(CompleteMaintenanceWindowLinkCommand.class));

    // Act & Assert
    listener.on(new MaintenanceWindowClosedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, "APP", STARTED, OTHER, "MANUAL", TECHNICIAN_ID, null));
    verifyNoInteractions(commands);
    assertThatCode(() -> listener.on(new MaintenanceWindowClosedIntegrationEvent(
        WINDOW_ID, EQUIPMENT_ID, "APP", STARTED, OTHER, "MANUAL", TECHNICIAN_ID, REQUEST_ID))).doesNotThrowAnyException();
  }
}
