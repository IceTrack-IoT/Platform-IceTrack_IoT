package pe.edu.upc.ice.track.platform.notifications.interfaces.events;

import java.time.Instant;

/**
 * Integration event published after a recipient's unread count changed: a notification arrived, or
 * one or several were read or dismissed.
 *
 * @param recipientProfileId the owner or technician profile whose count changed
 * @param unreadCount        the count after the change
 * @param changedAt          when it changed
 */
public record UnreadNotificationCountChangedIntegrationEvent(
    Long recipientProfileId,
    long unreadCount,
    Instant changedAt) {
}
