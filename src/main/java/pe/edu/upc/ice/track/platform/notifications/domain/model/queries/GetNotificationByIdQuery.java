package pe.edu.upc.ice.track.platform.notifications.domain.model.queries;

/**
 * Query to fetch a single notification by its identifier.
 *
 * @param notificationId  identifier of the notification
 * @param recipientUserId recipient resolved from the authenticated principal; a notification
 *                        addressed to anybody else is answered as absent
 */
public record GetNotificationByIdQuery(Long notificationId, Long recipientUserId) {
}
