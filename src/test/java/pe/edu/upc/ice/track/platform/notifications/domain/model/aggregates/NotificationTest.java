package pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.edu.upc.ice.track.platform.notifications.domain.model.commands.CreateNotificationCommand;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.MessageTemplate;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsDomainFixtures.*;

class NotificationTest {

  @Test
  @DisplayName("new Notification(CreateNotificationCommand) -> unread notification with the command's data (AAA)")
  void constructorBuildsUnreadNotification() {
    // Arrange
    var before = Instant.now();

    // Act
    var notification = notification();

    // Assert
    assertThat(notification.getId()).isNull();
    assertThat(notification.getRecipientUserId()).isEqualTo(RECIPIENT_ID);
    assertThat(notification.getEquipmentId().equipmentId()).isEqualTo(EQUIPMENT_ID);
    assertThat(notification.getDeviceId().deviceId()).isEqualTo(DEVICE_ID);
    assertThat(notification.getSourceAlertId().alertId()).isEqualTo(ALERT_ID);
    assertThat(notification.getServiceRequestId()).isEqualTo(SERVICE_REQUEST_ID);
    assertThat(notification.getMessage()).isEqualTo(MESSAGE);
    assertThat(notification.getType()).isEqualTo(NotificationType.TEMPERATURE_EXCURSION);
    assertThat(notification.getSeverity()).isEqualTo(NotificationSeverity.CRITICAL);
    assertThat(notification.isRead()).isFalse();
    assertThat(notification.getCreatedAt()).isAfterOrEqualTo(before);
    assertThat(notification.getReadAt()).isNull();
    assertThat(notification.getDismissedAt()).isNull();
  }

  @Test
  @DisplayName("new Notification(command without template) -> uses the untemplated message template (AAA)")
  void constructorDefaultsTemplate() {
    // Arrange & Act
    var notification = notification();

    // Assert
    assertThat(notification.getTemplate()).isEqualTo(MessageTemplate.untemplated());
  }

  @Test
  @DisplayName("new Notification(command with template) -> keeps the template (AAA)")
  void constructorKeepsTemplate() {
    // Arrange
    var template = MessageTemplate.of(MessageTemplate.ALERT_DEVICE_OFFLINE_RAISED).with("equipment_id", EQUIPMENT_ID).build();
    var command = new CreateNotificationCommand(RECIPIENT_ID, null, null, null, MESSAGE,
        NotificationType.DEVICE_OFFLINE, NotificationSeverity.WARNING, null, template);

    // Act
    var notification = new Notification(command);

    // Assert
    assertThat(notification.getTemplate()).isSameAs(template);
    assertThat(notification.getEquipmentId()).isNull();
    assertThat(notification.getDeviceId()).isNull();
    assertThat(notification.getSourceAlertId()).isNull();
    assertThat(notification.getServiceRequestId()).isNull();
  }

  @Test
  @DisplayName("new Notification(command without recipient) -> NullPointerException (AAA)")
  void constructorRejectsMissingRecipient() {
    // Arrange
    var command = new CreateNotificationCommand(null, null, null, null, MESSAGE,
        NotificationType.DEVICE_OFFLINE, NotificationSeverity.WARNING, null);

    // Act & Assert
    assertThatThrownBy(() -> new Notification(command)).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("recipientUserId");
  }

  @Test
  @DisplayName("new Notification(command without message) -> NullPointerException (AAA)")
  void constructorRejectsMissingMessage() {
    // Arrange
    var command = new CreateNotificationCommand(RECIPIENT_ID, null, null, null, null,
        NotificationType.DEVICE_OFFLINE, NotificationSeverity.WARNING, null);

    // Act & Assert
    assertThatThrownBy(() -> new Notification(command)).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("message");
  }

  @Test
  @DisplayName("new Notification(command without type) -> NullPointerException (AAA)")
  void constructorRejectsMissingType() {
    // Arrange
    var command = new CreateNotificationCommand(RECIPIENT_ID, null, null, null, MESSAGE,
        null, NotificationSeverity.WARNING, null);

    // Act & Assert
    assertThatThrownBy(() -> new Notification(command)).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("type");
  }

  @Test
  @DisplayName("new Notification(command without severity) -> NullPointerException (AAA)")
  void constructorRejectsMissingSeverity() {
    // Arrange
    var command = new CreateNotificationCommand(RECIPIENT_ID, null, null, null, MESSAGE,
        NotificationType.DEVICE_OFFLINE, null, null);

    // Act & Assert
    assertThatThrownBy(() -> new Notification(command)).isInstanceOf(NullPointerException.class)
        .hasMessageContaining("severity");
  }


  @Test
  @DisplayName("assignId(id) -> exposes the persistence id (AAA)")
  void assignIdSetsId() {
    // Arrange
    var notification = notification();

    // Act
    notification.assignId(NOTIFICATION_ID);

    // Assert
    assertThat(notification.getId()).isEqualTo(NOTIFICATION_ID);
  }

  @Test
  @DisplayName("markAsRead() -> flags it read and stamps readAt (AAA)")
  void markAsReadStampsReadAt() {
    // Arrange
    var notification = persistedNotification();
    var before = Instant.now();

    // Act
    notification.markAsRead();

    // Assert
    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getReadAt()).isAfterOrEqualTo(before);
  }

  @Test
  @DisplayName("markAsRead() twice -> keeps the first readAt (AAA)")
  void markAsReadIsIdempotent() {
    // Arrange
    var notification = rehydratedNotification(true, T0, null);

    // Act
    notification.markAsRead();

    // Assert
    assertThat(notification.getReadAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("dismiss() -> stamps dismissedAt without touching the read flag (AAA)")
  void dismissStampsDismissedAt() {
    // Arrange
    var notification = persistedNotification();
    var before = Instant.now();

    // Act
    notification.dismiss();

    // Assert
    assertThat(notification.getDismissedAt()).isAfterOrEqualTo(before);
    assertThat(notification.isRead()).isFalse();
  }

  @Test
  @DisplayName("dismiss() twice -> keeps the first dismissedAt (AAA)")
  void dismissIsIdempotent() {
    // Arrange
    var notification = rehydratedNotification(false, null, T0);

    // Act
    notification.dismiss();

    // Assert
    assertThat(notification.getDismissedAt()).isEqualTo(T0);
  }

  @Test
  @DisplayName("isAddressedTo(recipient) -> true only for its own recipient (AAA)")
  void isAddressedToMatchesRecipient() {
    // Arrange
    var notification = notification();

    // Act & Assert
    assertThat(notification.isAddressedTo(RECIPIENT_ID)).isTrue();
    assertThat(notification.isAddressedTo(OTHER_RECIPIENT_ID)).isFalse();
    assertThat(notification.isAddressedTo(null)).isFalse();
  }

  @Test
  @DisplayName("rehydrate(...) -> restores id, lifecycle instants and creation time (AAA)")
  void rehydrateRestoresState() {
    // Arrange & Act
    var notification = rehydratedNotification(true, T0.plusSeconds(60), T0.plusSeconds(120));

    // Assert
    assertThat(notification.getId()).isEqualTo(NOTIFICATION_ID);
    assertThat(notification.isRead()).isTrue();
    assertThat(notification.getCreatedAt()).isEqualTo(T0);
    assertThat(notification.getReadAt()).isEqualTo(T0.plusSeconds(60));
    assertThat(notification.getDismissedAt()).isEqualTo(T0.plusSeconds(120));
    assertThat(notification.getTemplate().key()).isEqualTo(MessageTemplate.ALERT_TEMPERATURE_EXCURSION_RAISED);
  }

  @Test
  @DisplayName("rehydrate(null createdAt) -> NullPointerException (AAA)")
  void rehydrateRequiresCreatedAt() {
    // Arrange & Act & Assert
    assertThatThrownBy(() -> Notification.rehydrate(NOTIFICATION_ID, RECIPIENT_ID, new EquipmentId(EQUIPMENT_ID), null,
        null, null, MESSAGE, null, NotificationType.DEVICE_OFFLINE, NotificationSeverity.WARNING, false, null, null,
        null)).isInstanceOf(NullPointerException.class).hasMessageContaining("createdAt");
  }
}
