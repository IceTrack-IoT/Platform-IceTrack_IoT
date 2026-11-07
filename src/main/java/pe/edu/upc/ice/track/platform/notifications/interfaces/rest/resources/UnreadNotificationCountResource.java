package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource carrying the caller's unread badge count.
 *
 * @param unreadCount number of the caller's notifications neither read nor dismissed
 */
@Schema(
    name = "UnreadNotificationCountResponse",
    description = "Number of the caller's notifications that are neither read nor dismissed",
    example = "{\"unread_count\": 7}")
public record UnreadNotificationCountResource(
    @Schema(description = "Number of notifications neither read nor dismissed", example = "7")
    long unreadCount
) {
}
