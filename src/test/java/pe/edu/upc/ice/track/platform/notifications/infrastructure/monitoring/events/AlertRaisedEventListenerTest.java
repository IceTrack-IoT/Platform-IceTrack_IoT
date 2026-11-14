package pe.edu.upc.ice.track.platform.notifications.infrastructure.monitoring.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.AlertRaisedIntegrationEvent;
import pe.edu.upc.ice.track.platform.notifications.application.commandservices.NotificationCommandService;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.NotifyAlertRaisedCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.AlertId;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.ALERT_ID;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.EQUIPMENT_ID;

@ExtendWith(MockitoExtension.class)
class AlertRaisedEventListenerTest {

  @Mock
  private NotificationCommandService notificationCommandService;
  @Mock
  private MonitoringContextFacade monitoringContextFacade;

  @InjectMocks
  private AlertRaisedEventListener listener;

  private NotifyAlertRaisedCommand dispatched() {
    var captor = ArgumentCaptor.forClass(NotifyAlertRaisedCommand.class);
    verify(notificationCommandService, times(1)).handle(captor.capture());
    return captor.getValue();
  }

  @Test
  @DisplayName("on(AlertRaisedIntegrationEvent excursion) -> maps type, severity and peak without asking silence (AAA)")
  void excursion() {
    // Arrange
    var event = new AlertRaisedIntegrationEvent(ALERT_ID, EQUIPMENT_ID, "TEMPERATURE_EXCURSION", "CRITICAL", 9.4);

    // Act
    listener.on(event);

    // Assert
    var command = dispatched();
    assertThat(command.alertId()).isEqualTo(new AlertId(ALERT_ID));
    assertThat(command.equipmentId()).isEqualTo(new EquipmentId(EQUIPMENT_ID));
    assertThat(command.type()).isEqualTo(NotificationType.TEMPERATURE_EXCURSION);
    assertThat(command.severity()).isEqualTo(NotificationSeverity.CRITICAL);
    assertThat(command.peakTemperature()).isEqualTo(9.4);
    assertThat(command.silentMinutes()).isNull();
    verifyNoInteractions(monitoringContextFacade);
  }

  @Test
  @DisplayName("on(AlertRaisedIntegrationEvent offline) -> adds the silence converted to whole minutes (AAA)")
  void offlineWithSilence() {
    // Arrange
    when(monitoringContextFacade.fetchOfflineSilenceSeconds(List.of(EQUIPMENT_ID)))
        .thenReturn(Map.of(EQUIPMENT_ID, 330L));

    // Act
    listener.on(new AlertRaisedIntegrationEvent(ALERT_ID, EQUIPMENT_ID, "DEVICE_OFFLINE", "WARNING", null));

    // Assert
    var command = dispatched();
    assertThat(command.type()).isEqualTo(NotificationType.DEVICE_OFFLINE);
    assertThat(command.severity()).isEqualTo(NotificationSeverity.WARNING);
    assertThat(command.silentMinutes()).isEqualTo(5L);
  }

  @Test
  @DisplayName("on(AlertRaisedIntegrationEvent offline) without known silence -> silentMinutes is null (AAA)")
  void offlineWithoutSilence() {
    // Arrange
    when(monitoringContextFacade.fetchOfflineSilenceSeconds(List.of(EQUIPMENT_ID))).thenReturn(Map.of());

    // Act
    listener.on(new AlertRaisedIntegrationEvent(ALERT_ID, EQUIPMENT_ID, "DEVICE_OFFLINE", "WARNING", null));

    // Assert
    assertThat(dispatched().silentMinutes()).isNull();
  }

  @Test
  @DisplayName("on(AlertRaisedIntegrationEvent sensor failure, INFO) -> maps both enums (AAA)")
  void sensorFailureInfo() {
    // Act
    listener.on(new AlertRaisedIntegrationEvent(ALERT_ID, EQUIPMENT_ID, "SENSOR_FAILURE", "INFO", null));

    // Assert
    var command = dispatched();
    assertThat(command.type()).isEqualTo(NotificationType.SENSOR_FAILURE);
    assertThat(command.severity()).isEqualTo(NotificationSeverity.INFO);
  }

  @Test
  @DisplayName("on(AlertRaisedIntegrationEvent unmapped type) -> IllegalArgumentException, nothing dispatched (AAA)")
  void unmappedType() {
    // Arrange
    var event = new AlertRaisedIntegrationEvent(ALERT_ID, EQUIPMENT_ID, "UNKNOWN", "INFO", null);

    // Act & Assert
    assertThatThrownBy(() -> listener.on(event))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Unmapped alert type: UNKNOWN");
    verifyNoInteractions(notificationCommandService);
  }

  @Test
  @DisplayName("on(AlertRaisedIntegrationEvent unmapped severity) -> IllegalArgumentException, nothing dispatched (AAA)")
  void unmappedSeverity() {
    // Arrange
    var event = new AlertRaisedIntegrationEvent(ALERT_ID, EQUIPMENT_ID, "SENSOR_FAILURE", "FATAL", null);

    // Act & Assert
    assertThatThrownBy(() -> listener.on(event))
        .isInstanceOf(IllegalArgumentException.class).hasMessage("Unmapped alert severity: FATAL");
    verifyNoInteractions(notificationCommandService);
  }
}
