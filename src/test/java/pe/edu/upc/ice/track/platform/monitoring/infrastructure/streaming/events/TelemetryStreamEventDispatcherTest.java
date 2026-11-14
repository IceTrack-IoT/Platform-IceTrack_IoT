package pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.events;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.outboundservices.acl.ExternalAssetManagementService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.AlertStatusChangedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.events.ReadingRecordedEvent;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertSeverity;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.infrastructure.streaming.sse.TelemetryStreamRegistry;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertStatusChangedStreamResource;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ReadingRecordedStreamResource;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.monitoring.test.fixtures.MonitoringDomainFixtures.*;

/**
 * The SSE registry is mocked: its emitter handling is infrastructure, while the dispatcher's own logic is
 * deciding whether and for whom an event is published.
 */
@ExtendWith(MockitoExtension.class)
class TelemetryStreamEventDispatcherTest {

  @Mock
  private TelemetryStreamRegistry telemetryStreamRegistry;
  @Mock
  private ExternalAssetManagementService externalAssetManagementService;

  @InjectMocks
  private TelemetryStreamEventDispatcher dispatcher;

  private static ReadingRecordedEvent readingEvent() {
    return new ReadingRecordedEvent(READING_UID, EQUIPMENT_ID, DEVICE_ID, 4.0, 6.0, 5.0, 55.0, 40, T0, T0, false);
  }

  private static AlertStatusChangedEvent alertEvent() {
    return new AlertStatusChangedEvent(ALERT_ID, EQUIPMENT_ID, AlertType.TEMPERATURE_EXCURSION,
        AlertSeverity.CRITICAL, null, AlertStatus.OPEN, TEMPERATURE, T0);
  }

  @Test
  @DisplayName("on(ReadingRecordedEvent) followed owner -> publishes a telemetry-update with the reading payload (AAA)")
  void publishesReadingToFollowingOwner() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(true);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(telemetryStreamRegistry.isFollowed(OWNER_PROFILE_ID)).thenReturn(true);

    // Act
    dispatcher.on(readingEvent());

    // Assert
    var payload = ArgumentCaptor.forClass(Object.class);
    verify(telemetryStreamRegistry, times(1)).publish(
        org.mockito.ArgumentMatchers.eq(OWNER_PROFILE_ID),
        org.mockito.ArgumentMatchers.eq(TelemetryStreamEventDispatcher.TELEMETRY_UPDATE_EVENT),
        payload.capture());
    assertThat(payload.getValue()).isInstanceOf(ReadingRecordedStreamResource.class);
    assertThat(((ReadingRecordedStreamResource) payload.getValue()).equipmentId()).isEqualTo(EQUIPMENT_ID);
  }

  @Test
  @DisplayName("on(AlertStatusChangedEvent) followed owner -> publishes a telemetry-update with the alert payload (AAA)")
  void publishesAlertToFollowingOwner() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(true);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(telemetryStreamRegistry.isFollowed(OWNER_PROFILE_ID)).thenReturn(true);

    // Act
    dispatcher.on(alertEvent());

    // Assert
    var payload = ArgumentCaptor.forClass(Object.class);
    verify(telemetryStreamRegistry, times(1)).publish(
        org.mockito.ArgumentMatchers.eq(OWNER_PROFILE_ID),
        org.mockito.ArgumentMatchers.eq(TelemetryStreamEventDispatcher.TELEMETRY_UPDATE_EVENT),
        payload.capture());
    assertThat(payload.getValue()).isInstanceOf(AlertStatusChangedStreamResource.class);
    assertThat(((AlertStatusChangedStreamResource) payload.getValue()).alertId()).isEqualTo(ALERT_ID);
  }

  @Test
  @DisplayName("on(event) with nobody connected -> does not even resolve the owner (AAA)")
  void skipsWhenNoSubscribers() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(false);

    // Act
    dispatcher.on(readingEvent());

    // Assert
    verifyNoInteractions(externalAssetManagementService);
    verify(telemetryStreamRegistry, never()).publish(any(), anyString(), any());
  }

  @Test
  @DisplayName("on(event) with unresolved owner -> nothing is published (AAA)")
  void skipsWhenOwnerCannotBeResolved() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(true);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.empty());

    // Act
    dispatcher.on(alertEvent());

    // Assert
    verify(telemetryStreamRegistry, never()).isFollowed(any());
    verify(telemetryStreamRegistry, never()).publish(any(), anyString(), any());
  }

  @Test
  @DisplayName("on(event) for an owner nobody follows -> nothing is published (AAA)")
  void skipsWhenOwnerIsNotFollowed() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(true);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(telemetryStreamRegistry.isFollowed(OWNER_PROFILE_ID)).thenReturn(false);

    // Act
    dispatcher.on(readingEvent());

    // Assert
    verify(telemetryStreamRegistry, never()).publish(any(), anyString(), any());
  }

  @Test
  @DisplayName("on(event) when publishing fails -> the error is contained (AAA)")
  void containsPublishFailures() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(true);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenReturn(Optional.of(OWNER_PROFILE_ID));
    when(telemetryStreamRegistry.isFollowed(OWNER_PROFILE_ID)).thenReturn(true);
    doThrow(new IllegalStateException("emitter closed")).when(telemetryStreamRegistry)
        .publish(any(), anyString(), any());

    // Act & Assert
    assertThatCode(() -> dispatcher.on(readingEvent())).doesNotThrowAnyException();
  }

  @Test
  @DisplayName("on(event) when the owner lookup fails -> the error is contained (AAA)")
  void containsOwnerLookupFailures() {
    // Arrange
    when(telemetryStreamRegistry.hasSubscribers()).thenReturn(true);
    when(externalAssetManagementService.fetchOwnerId(EQUIPMENT_ID)).thenThrow(new IllegalStateException("db down"));

    // Act & Assert
    assertThatCode(() -> dispatcher.on(alertEvent())).doesNotThrowAnyException();
    verify(telemetryStreamRegistry, never()).publish(any(), anyString(), any());
  }
}
