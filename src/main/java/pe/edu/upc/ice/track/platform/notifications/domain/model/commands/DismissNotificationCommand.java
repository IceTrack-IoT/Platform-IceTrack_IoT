package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/**
 * Command to dismiss a notification from the recipient's active inbox.
 *
 * @param notificationId  identifier of the notification
 * @param recipientUserId recipient resolved from the authenticated principal; the command applies
 *                        only when the notification is addressed to it
 */
public record DismissNotificationCommand(Long notificationId, Long recipientUserId) {
}
