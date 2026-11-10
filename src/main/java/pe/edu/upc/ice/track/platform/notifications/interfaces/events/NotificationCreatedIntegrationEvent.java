package pe.edu.upc.ice.track.platform.notifications.interfaces.events;

import java.time.Instant;

/**
 * Integration event published after a notification was stored. It carries no message text, only
 * what a client needs to refresh its inbox and badge.
 *
 * @param notificationId     the notification
 * @param recipientProfileId the owner or technician profile it is addressed to
 * @param type               the kind of notification, by its constant name
 * @param severity           its urgency, by its constant name
 * @param equipmentId        the unit it concerns; may be {@code null}
 * @param sourceAlertId      the alert that raised it; may be {@code null}
 * @param createdAt          when it was stored
 * @param unreadCount        the recipient's unread count after this notification
 */
public record NotificationCreatedIntegrationEvent(
    Long notificationId,
    Long recipientProfileId,
    String type,
    String severity,
    Long equipmentId,
    Long sourceAlertId,
    Instant createdAt,
    long unreadCount) {
}
