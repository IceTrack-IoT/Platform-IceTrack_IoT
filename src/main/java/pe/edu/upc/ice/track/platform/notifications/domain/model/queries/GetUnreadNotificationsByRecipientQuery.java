package pe.edu.upc.ice.track.platform.notifications.domain.model.queries;

/** Query to list only the unread notifications addressed to a recipient. */
public record GetUnreadNotificationsByRecipientQuery(Long recipientUserId) {
}
