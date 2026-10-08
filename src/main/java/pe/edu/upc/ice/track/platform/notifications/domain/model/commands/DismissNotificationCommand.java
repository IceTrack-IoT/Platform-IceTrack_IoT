package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/** Command to dismiss a notification from the recipient's active inbox. */
public record DismissNotificationCommand(Long notificationId) {
}
