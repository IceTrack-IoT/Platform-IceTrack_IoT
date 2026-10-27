package pe.edu.upc.ice.track.platform.notifications.domain.model.events;

import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationSeverity;
import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.NotificationType;

/**
 * Internal event: a notification was created and persisted, so it can now be pushed to the
 * recipient's devices. Carries only what a push needs; it never leaves the notifications context.
 */
public record NotificationCreatedEvent(
    Long notificationId,
    Long recipientUserId,
    String message,
    NotificationType type,
    NotificationSeverity severity,
    Long equipmentId,
    Long sourceAlertId) {
}
