package pe.edu.upc.ice.track.platform.notifications.infrastructure.monitoring.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowClosedIntegrationEvent;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.MaintenanceWindowOpenedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowClosedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyMaintenanceWindowOpenedCommand;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowNotificationEventListenerTest {

  @Mock
  private NotificationCommandService notificationCommandService;

  @InjectMocks
  private MaintenanceWindowNotificationEventListener listener;

  private static MaintenanceWindowOpenedIntegrationEvent opened() {
    return new MaintenanceWindowOpenedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, "APP", T0, T0.plusSeconds(3600),
        TECHNICIAN_ID, SERVICE_REQUEST_ID);
  }

  private static MaintenanceWindowClosedIntegrationEvent closed() {
    return new MaintenanceWindowClosedIntegrationEvent(WINDOW_ID, EQUIPMENT_ID, "APP", T0, T0.plusSeconds(600),
        "MANUAL_APP", TECHNICIAN_ID, SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("on(MaintenanceWindowOpenedIntegrationEvent) -> dispatches the matching opened command (AAA)")
  void openedDispatches() {
    // Act
    listener.on(opened());

    // Assert
    var captor = ArgumentCaptor.forClass(NotifyMaintenanceWindowOpenedCommand.class);
    verify(notificationCommandService, times(1)).handle(captor.capture());
    var command = captor.getValue();
    assertThat(command.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(command.equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(command.origin()).isEqualTo("APP");
    assertThat(command.startedAt()).isEqualTo(T0);
    assertThat(command.expiresAt()).isEqualTo(T0.plusSeconds(3600));
    assertThat(command.technicianProfileId()).isEqualTo(TECHNICIAN_ID);
    assertThat(command.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("on(MaintenanceWindowClosedIntegrationEvent) -> dispatches the matching closed command (AAA)")
  void closedDispatches() {
    // Act
    listener.on(closed());

    // Assert
    var captor = ArgumentCaptor.forClass(NotifyMaintenanceWindowClosedCommand.class);
    verify(notificationCommandService, times(1)).handle(captor.capture());
    var command = captor.getValue();
    assertThat(command.maintenanceWindowId()).isEqualTo(WINDOW_ID);
    assertThat(command.endedAt()).isEqualTo(T0.plusSeconds(600));
    assertThat(command.closureReason()).isEqualTo("MANUAL_APP");
    assertThat(command.serviceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
  }

  @Test
  @DisplayName("on(MaintenanceWindowOpenedIntegrationEvent) service failure -> exception is swallowed (AAA)")
  void openedFailureIsSwallowed() {
    // Arrange
    doThrow(new IllegalStateException("boom")).when(notificationCommandService)
        .handle(any(NotifyMaintenanceWindowOpenedCommand.class));

    // Act & Assert
    assertThatCode(() -> listener.on(opened())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("on(MaintenanceWindowClosedIntegrationEvent) service failure -> exception is swallowed (AAA)")
  void closedFailureIsSwallowed() {
    // Arrange
    doThrow(new IllegalStateException("boom")).when(notificationCommandService)
        .handle(any(NotifyMaintenanceWindowClosedCommand.class));

    // Act & Assert
    assertThatCode(() -> listener.on(closed())).doesNotThrowAnyException();
  }
}
