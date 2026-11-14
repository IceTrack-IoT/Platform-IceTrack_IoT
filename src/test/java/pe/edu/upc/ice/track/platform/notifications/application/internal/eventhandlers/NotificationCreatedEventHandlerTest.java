package pe.edu.upc.ice.track.platform.notifications.application.internal.eventhandlers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upc.ice.track.platform.notifications.application.internal.outboundservices.push.PushGateway;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.model.events.NotificationCreatedEvent;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.notifications.domain.repositories.DeviceTokenRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

@ExtendWith(MockitoExtension.class)
class NotificationCreatedEventHandlerTest {

  @Mock
  private DeviceTokenRepository deviceTokenRepository;
  @Mock
  private PushGateway pushGateway;

  @InjectMocks
  private NotificationCreatedEventHandler handler;

  private static NotificationCreatedEvent event(NotificationSeverity severity, Long equipmentId, Long alertId) {
    return new NotificationCreatedEvent(NOTIFICATION_ID, RECIPIENT_ID, MESSAGE, NotificationType.TEMPERATURE_EXCURSION,
        severity, equipmentId, alertId);
  }

  @SuppressWarnings("unchecked")
  private Map<String, String> sentData(String expectedTitle) {
    var data = ArgumentCaptor.forClass(Map.class);
    verify(pushGateway, times(1)).send(anyCollection(), org.mockito.ArgumentMatchers.eq(expectedTitle),
        org.mockito.ArgumentMatchers.eq(MESSAGE), data.capture());
    return data.getValue();
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) without registered devices -> nothing is sent (AAA)")
  void noDevices() {
    // Arrange
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenReturn(List.of());

    // Act
    handler.on(event(NotificationSeverity.CRITICAL, EQUIPMENT_ID, ALERT_ID));

    // Assert
    verifyNoInteractions(pushGateway);
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) critical -> pushes to all tokens with critical title and full payload (AAA)")
  void criticalPush() {
    // Arrange
    var second = DeviceToken.rehydrate(2L, RECIPIENT_ID, "second-token", DevicePlatform.IOS, null);
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenReturn(List.of(persistedDeviceToken(), second));
    when(pushGateway.send(anyCollection(), anyString(), anyString(), anyMap())).thenReturn(Set.of());

    // Act
    handler.on(event(NotificationSeverity.CRITICAL, EQUIPMENT_ID, ALERT_ID));

    // Assert
    var tokens = ArgumentCaptor.forClass(Collection.class);
    verify(pushGateway).send(tokens.capture(), anyString(), anyString(), anyMap());
    assertThat(new java.util.ArrayList<Object>((Collection<?>) tokens.getValue())).containsExactly(persistedDeviceToken().getToken(), "second-token");
    assertThat(sentData("IceTrack - Critical alert")).containsOnly(
        org.assertj.core.api.Assertions.entry("notification_id", "12"),
        org.assertj.core.api.Assertions.entry("type", "TEMPERATURE_EXCURSION"),
        org.assertj.core.api.Assertions.entry("severity", "CRITICAL"),
        org.assertj.core.api.Assertions.entry("equipment_id", "10"),
        org.assertj.core.api.Assertions.entry("alert_id", "301"));
    verify(deviceTokenRepository, never()).deleteByTokenIn(any());
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) warning -> warning title (AAA)")
  void warningTitle() {
    // Arrange
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenReturn(List.of(persistedDeviceToken()));
    when(pushGateway.send(anyCollection(), anyString(), anyString(), anyMap())).thenReturn(Set.of());

    // Act
    handler.on(event(NotificationSeverity.WARNING, EQUIPMENT_ID, ALERT_ID));

    // Assert
    sentData("IceTrack - Warning");
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) info without equipment or alert -> plain title and minimal payload (AAA)")
  void infoMinimalPayload() {
    // Arrange
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenReturn(List.of(persistedDeviceToken()));
    when(pushGateway.send(anyCollection(), anyString(), anyString(), anyMap())).thenReturn(Set.of());

    // Act
    handler.on(event(NotificationSeverity.INFO, null, null));

    // Assert
    assertThat(sentData("IceTrack")).containsOnlyKeys("notification_id", "type", "severity");
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) provider reports dead tokens -> they are deleted (AAA)")
  void invalidTokensAreDeleted() {
    // Arrange
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenReturn(List.of(persistedDeviceToken()));
    var dead = Set.of(persistedDeviceToken().getToken());
    when(pushGateway.send(anyCollection(), anyString(), anyString(), anyMap())).thenReturn(dead);

    // Act
    handler.on(event(NotificationSeverity.CRITICAL, EQUIPMENT_ID, ALERT_ID));

    // Assert
    verify(deviceTokenRepository, times(1)).deleteByTokenIn(dead);
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) gateway failure -> exception is swallowed (AAA)")
  void gatewayFailureDoesNotEscape() {
    // Arrange
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenReturn(List.of(persistedDeviceToken()));
    when(pushGateway.send(anyCollection(), anyString(), anyString(), anyMap()))
        .thenThrow(new IllegalStateException("provider down"));

    // Act & Assert
    assertThatCode(() -> handler.on(event(NotificationSeverity.CRITICAL, EQUIPMENT_ID, ALERT_ID)))
        .doesNotThrowAnyException();
    verify(deviceTokenRepository, never()).deleteByTokenIn(any());
  }

  @Test
  @DisplayName("on(NotificationCreatedEvent) repository failure -> exception is swallowed (AAA)")
  void repositoryFailureDoesNotEscape() {
    // Arrange
    when(deviceTokenRepository.findByRecipientUserId(RECIPIENT_ID)).thenThrow(new IllegalStateException("db down"));

    // Act & Assert
    assertThatCode(() -> handler.on(event(NotificationSeverity.CRITICAL, EQUIPMENT_ID, ALERT_ID)))
        .doesNotThrowAnyException();
    verifyNoInteractions(pushGateway);
  }
}
