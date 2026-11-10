package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Data of the {@code notification-created} stream event: a notification addressed to the stream owner
 * was stored. It carries no message text; the client reads the notification over the REST API.
 */
@Schema(
    name = "NotificationCreatedStreamEvent",
    description = "Data of the notification-created event",
    example = "{\"notification_id\": 12, \"type\": \"TEMPERATURE_EXCURSION\", \"severity\": \"CRITICAL\", "
        + "\"equipment_id\": 10, \"source_alert_id\": 301, \"created_at\": \"2026-11-09T10:02:01Z\", \"unread_count\": 4}")
public record NotificationCreatedStreamResource(
    @Schema(description = "Identifier of the notification", example = "12")
    Long notificationId,

    @Schema(description = "Kind of notification (NotificationType)", example = "TEMPERATURE_EXCURSION")
    String type,

    @Schema(description = "Urgency (NotificationSeverity)", example = "CRITICAL")
    String severity,

    @Schema(description = "Unit the notification concerns", example = "10", nullable = true)
    Long equipmentId,

    @Schema(description = "Alert that raised the notification", example = "301", nullable = true)
    Long sourceAlertId,

    @Schema(description = "When the notification was stored", example = "2026-11-09T10:02:01Z")
    Instant createdAt,

    @Schema(description = "The owner's unread count after this notification", example = "4")
    long unreadCount
) {
}
