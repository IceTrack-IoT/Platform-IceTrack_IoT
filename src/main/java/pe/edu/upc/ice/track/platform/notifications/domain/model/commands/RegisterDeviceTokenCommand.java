package pe.edu.upc.ice.track.platform.notifications.domain.model.commands;

import pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects.DevicePlatform;

/**
 * Command to register (or refresh) the FCM registration token of one of a recipient's devices.
 *
 * @param recipientUserId owner the device belongs to; required
 * @param token           FCM registration token issued to the app instance; required
 * @param platform        platform of the app instance; required
 */
public record RegisterDeviceTokenCommand(Long recipientUserId, String token, DevicePlatform platform) {
}
