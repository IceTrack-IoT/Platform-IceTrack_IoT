package pe.edu.upc.ice.track.platform.notifications.test.fixtures;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.DeviceToken;
import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;
import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.EquipmentId;

import java.time.Instant;

import static pe.edu.upc.ice.track.platform.notifications.test.fixtures.NotificationsCommandFixtures.createNotificationCommand;

public final class NotificationsDomainFixtures {

  private NotificationsDomainFixtures() {
    throw new AssertionError("NotificationsDomainFixtures is a utility class and must not be instantiated.");
  }

  public static final Long RECIPIENT_ID = 42L;
  public static final Long OTHER_RECIPIENT_ID = 43L;
  public static final Long NOTIFICATION_ID = 12L;
  public static final Long EQUIPMENT_ID = 10L;
  public static final Long DEVICE_ID = 7L;
  public static final Long ALERT_ID = 301L;
  public static final Long SERVICE_REQUEST_ID = 77L;
  public static final Long TECHNICIAN_ID = 66L;
  public static final Long PREVIOUS_TECHNICIAN_ID = 67L;
  public static final Long OWNER_ID = 55L;
  public static final Long SITE_ID = 5L;
  public static final Long WINDOW_ID = 300L;
  public static final String USERNAME = "jperez";
  public static final String EQUIPMENT_NAME = "Cold Room #01";
  public static final String TOKEN = "fcm-token-abc123";
  public static final String MESSAGE = "Cold Room #01 reached 9.4 °C, above its maximum of 8.0 °C";
  public static final Instant T0 = Instant.parse("2026-01-01T10:00:00Z");

  /** An unsaved, unread alert notification for {@link #RECIPIENT_ID}. */
  public static Notification notification() {
    return new Notification(createNotificationCommand());
  }

  public static Notification persistedNotification() {
    var notification = notification();
    notification.assignId(NOTIFICATION_ID);
    return notification;
  }

  public static Notification readNotification() {
    var notification = persistedNotification();
    notification.markAsRead();
    return notification;
  }

  public static Notification dismissedNotification() {
    var notification = persistedNotification();
    notification.dismiss();
    return notification;
  }

  public static Notification rehydratedNotification(boolean read, Instant readAt, Instant dismissedAt) {
    return Notification.rehydrate(NOTIFICATION_ID, RECIPIENT_ID, new EquipmentId(EQUIPMENT_ID), new DeviceId(DEVICE_ID),
        new AlertId(ALERT_ID), SERVICE_REQUEST_ID, MESSAGE, MessageTemplate.of(MessageTemplate.ALERT_TEMPERATURE_EXCURSION_RAISED)
            .with("equipment_id", EQUIPMENT_ID).build(),
        NotificationType.TEMPERATURE_EXCURSION, NotificationSeverity.CRITICAL, read, T0, readAt, dismissedAt);
  }

  public static DeviceToken deviceToken() {
    return new DeviceToken(RECIPIENT_ID, TOKEN, DevicePlatform.ANDROID);
  }

  public static DeviceToken persistedDeviceToken() {
    return DeviceToken.rehydrate(1L, RECIPIENT_ID, TOKEN, DevicePlatform.ANDROID, T0);
  }
}
