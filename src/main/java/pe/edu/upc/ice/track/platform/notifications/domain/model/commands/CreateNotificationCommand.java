package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.*;

/**
 * Command to create a new notification for a recipient.
 *
 * @param recipientUserId identifier of the user the notification is addressed to; required
 * @param equipmentId     equipment the notification concerns, may be {@code null}
 * @param deviceId        device the notification concerns, may be {@code null}
 * @param sourceAlertId   identifier of the originating Alert in Monitoring and Alerting
 *                        Management, may be {@code null} when the notification has no alert
 *                        as its source
 * @param message         final, already-resolved text shown to the recipient; required
 * @param type            kind of notification; required
 * @param severity        urgency of the notification; required
 */
public record CreateNotificationCommand(
    Long recipientUserId,
    EquipmentId equipmentId,
    DeviceId deviceId,
    AlertId sourceAlertId,
    String message,
    NotificationType type,
    NotificationSeverity severity) {
}
