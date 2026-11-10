package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Outbound REST resource answering a bulk read: how many notifications changed, and the unread
 * badge count afterward, so the client updates the badge without another request.
 *
 * @param updatedCount notifications that changed from unread to read
 * @param unreadCount  the caller's notifications neither read nor dismissed after the change
 */
@Schema(
    name = "BulkReadResultResponse",
    description = "Outcome of marking several notifications as read",
    example = "{\"updated_count\": 7, \"unread_count\": 0}")
public record BulkReadResultResource(
    @Schema(description = "Notifications that changed from unread to read", example = "7")
    long updatedCount,

    @Schema(description = "Notifications neither read nor dismissed after the change", example = "0")
    long unreadCount
) {
}
