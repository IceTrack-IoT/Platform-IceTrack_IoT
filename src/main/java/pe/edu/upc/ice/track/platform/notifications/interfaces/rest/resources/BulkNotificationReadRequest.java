package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Resource received to mark a selection of the caller's notifications as read.
 *
 * @param notificationIds identifiers of the notifications to mark, from 1 to 100; duplicates count
 *                        once
 */
@Schema(
    name = "BulkNotificationReadRequest",
    description = "Notifications of the caller to mark as read",
    example = "{\"notification_ids\": [12, 13, 20]}")
public record BulkNotificationReadRequest(
    @NotNull(message = "{validation.not-null}")
    @Size(min = 1, max = 100, message = "{validation.notification-ids.size}")
    @ArraySchema(
        arraySchema = @Schema(description = "Identifiers of the notifications to mark as read; identifiers not "
            + "addressed to the caller, already read or dismissed are skipped"),
        schema = @Schema(example = "12"),
        minItems = 1,
        maxItems = 100)
    List<@NotNull(message = "{validation.not-null}") @Positive(message = "{validation.positive}") Long> notificationIds
) {
}
