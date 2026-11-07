package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/**
 * Command to mark a notification as read.
 *
 * @param notificationId  identifier of the notification
 * @param recipientUserId recipient resolved from the authenticated principal; the command applies
 *                        only when the notification is addressed to it
 */
public record MarkNotificationAsReadCommand(Long notificationId, Long recipientUserId) {
}
