package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Outbound REST resource representing a notification. */
@Schema(
    name = "NotificationResponse",
    description = "Notification delivered to a platform account",
    example = "{\"id\": 12, \"recipient_user_id\": 42, \"equipment_id\": 10, \"device_id\": 7, "
        + "\"source_alert_id\": 301, \"message\": \"Equipment 10 is above its maximum temperature\", "
        + "\"type\": \"TEMPERATURE_EXCURSION\", \"severity\": \"CRITICAL\", \"is_read\": false, "
        + "\"read_at\": null, \"dismissed_at\": null}"
)
public record NotificationResource(
    @Schema(description = "Notification unique identifier", example = "12")
    Long id,

    @Schema(description = "Identifier of the user the notification is addressed to", example = "42")
    Long recipientUserId,

    @Schema(description = "Identifier of the equipment the notification concerns", example = "10",
        nullable = true)
    Long equipmentId,

    @Schema(description = "Identifier of the device the notification concerns", example = "7",
        nullable = true)
    Long deviceId,

    @Schema(description = "Identifier of the alert that triggered the notification", example = "301",
        nullable = true)
    Long sourceAlertId,

    @Schema(description = "Human-readable notification text",
        example = "Equipment 10 is above its maximum temperature")
    String message,

    @Schema(description = "Kind of event that produced the notification", example = "TEMPERATURE_EXCURSION",
        allowableValues = {"MAINTENANCE_REMINDER", "TEMPERATURE_EXCURSION", "DEVICE_OFFLINE",
            "LOW_BATTERY", "SENSOR_FAILURE", "SERVICE_REQUEST_UPDATE"})
    String type,

    @Schema(description = "Urgency of the notification",
        example = "CRITICAL", allowableValues = {"INFO", "WARNING", "CRITICAL"})
    String severity,

    @Schema(description = "Whether the user has already read the notification", example = "false")
    boolean isRead,

    @Schema(description = "When the notification was read; null while unread",
        example = "2026-10-23T17:00:00", nullable = true)
    LocalDateTime readAt,

    @Schema(description = "When the user dismissed the notification; null while not dismissed",
        example = "2026-10-23T17:05:00", nullable = true)
    LocalDateTime dismissedAt) {
}
