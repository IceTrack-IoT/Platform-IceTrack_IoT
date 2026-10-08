package pe.edu.upc.ice.track.platform.notifications.domain.model.queries;

/** Query to list every notification addressed to a recipient, read or not. */
public record GetNotificationsByRecipientQuery(Long recipientUserId) {
}
