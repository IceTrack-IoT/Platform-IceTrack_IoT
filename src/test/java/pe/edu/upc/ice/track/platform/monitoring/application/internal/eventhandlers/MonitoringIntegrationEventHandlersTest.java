package pe.edu.upc.ice.track.platform.monitoring.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.*;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ClosureReason;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.MaintenanceOrigin;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.events.*;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

/** The three publishing handlers translate a domain event into the integration event other contexts listen to. */
class MonitoringIntegrationEventHandlersTest {

  private static Object published(ApplicationEventPublisher publisher) {
    var captor = ArgumentCaptor.forClass(Object.class);
    verify(publisher, times(1)).publishEvent(captor.capture());
    return captor.getValue();
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class AlertRaised {
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private AlertRaisedEventHandler handler;

    @Test
    @DisplayName("on(AlertRaisedEvent) -> publishes AlertRaisedIntegrationEvent with type and severity names (AAA)")
    void publishesIntegrationEvent() {
      // Arrange
      var event = new AlertRaisedEvent(ALERT_ID, EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION,
          AlertSeverity.CRITICAL, TEMPERATURE);

      // Act
      handler.on(event);

      // Assert
      assertThat(published(eventPublisher)).isEqualTo(new AlertRaisedIntegrationEvent(
          ALERT_ID, EQUIPMENT_ID, "TEMPERATURE_EXCURSION", "CRITICAL", TEMPERATURE));
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class AlertPolicyUpdated {
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private AlertPolicyUpdatedEventHandler handler;

    @Test
    @DisplayName("on(AlertPolicyUpdatedEvent) -> publishes AlertPolicyUpdatedIntegrationEvent with the same values (AAA)")
    void publishesIntegrationEvent() {
      // Arrange
      var event = new AlertPolicyUpdatedEvent(EQUIPMENT_ID, 5, 1.5, 4);

      // Act
      handler.on(event);

      // Assert
      assertThat(published(eventPublisher))
          .isEqualTo(new AlertPolicyUpdatedIntegrationEvent(EQUIPMENT_ID, 5, 1.5, 4));
    }
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class MaintenanceWindow {
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private MaintenanceWindowEventHandler handler;

    @Test
    @DisplayName("on(MaintenanceWindowOpenedEvent) -> publishes the opened integration event (AAA)")
    void publishesOpened() {
      // Arrange
      var expiresAt = T0.plus(Duration.ofHours(2));
      var event = new MaintenanceWindowOpenedEvent(WINDOW_ID, START_EVENT_UID, EQUIPMENT_ID, MaintenanceOrigin.APP,
          T0, expiresAt, TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID);

      // Act
      handler.on(event);

      // Assert
      var integration = (MaintenanceWindowOpenedIntegrationEvent) published(eventPublisher);
      assertThat(integration).isEqualTo(new MaintenanceWindowOpenedIntegrationEvent(
          WINDOW_ID, EQUIPMENT_ID, "APP", T0, expiresAt, TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID));
      assertThat(integration.unlinked()).isFalse();
    }

    @Test
    @DisplayName("on(MaintenanceWindowOpenedEvent) without order -> integration event is unlinked (AAA)")
    void publishesUnlinkedOpened() {
      // Arrange
      var event = new MaintenanceWindowOpenedEvent(WINDOW_ID, START_EVENT_UID, EQUIPMENT_ID,
          MaintenanceOrigin.BUTTON, T0, T0.plusSeconds(1800), null, null);

      // Act
      handler.on(event);

      // Assert
      assertThat(((MaintenanceWindowOpenedIntegrationEvent) published(eventPublisher)).unlinked()).isTrue();
    }

    @Test
    @DisplayName("on(MaintenanceWindowPromotedEvent) -> publishes the promoted integration event (AAA)")
    void publishesPromoted() {
      // Arrange
      var promotedAt = T0.plusSeconds(60);
      var expiresAt = promotedAt.plus(Duration.ofHours(2));
      var event = new MaintenanceWindowPromotedEvent(WINDOW_ID, EQUIPMENT_ID, T0, promotedAt, expiresAt,
          TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID);

      // Act
      handler.on(event);

      // Assert
      assertThat(published(eventPublisher)).isEqualTo(new MaintenanceWindowPromotedIntegrationEvent(
          WINDOW_ID, EQUIPMENT_ID, T0, promotedAt, expiresAt, TECHNICIAN_PROFILE_ID, SERVICE_REQUEST_ID));
    }

    @Test
    @DisplayName("on(MaintenanceWindowClosedEvent) -> publishes the closed integration event with enum names (AAA)")
    void publishesClosed() {
      // Arrange
      var endedAt = T0.plusSeconds(600);
      var event = new MaintenanceWindowClosedEvent(WINDOW_ID, EQUIPMENT_ID, MaintenanceOrigin.BUTTON, T0, endedAt,
          ClosureReason.MANUAL_BUTTON, null, null);

      // Act
      handler.on(event);

      // Assert
      assertThat(published(eventPublisher)).isEqualTo(new MaintenanceWindowClosedIntegrationEvent(
          WINDOW_ID, EQUIPMENT_ID, "BUTTON", T0, endedAt, "MANUAL_BUTTON", null, null));
    }
  }
}
