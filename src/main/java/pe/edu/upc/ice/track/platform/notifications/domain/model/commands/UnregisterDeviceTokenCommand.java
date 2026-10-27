package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

/**
 * Command to stop pushing to a device (for example on logout).
 *
 * @param recipientUserId owner issuing the request; a token that belongs to someone else is left alone
 * @param token           FCM registration token to remove
 */
public record UnregisterDeviceTokenCommand(Long recipientUserId, String token) {
}
