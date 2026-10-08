package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/** Command to mark a notification as read. */
public record MarkNotificationAsReadCommand(Long notificationId) {
}
