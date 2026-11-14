package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Map;

/**
 * Outbound REST resource representing a notification.
 *
 * <p>{@code message} is composed once, in English, when the notification is raised. {@code message_key}
 * and {@code message_params} carry the same text in machine form, so a client renders it in the
 * user's language and falls back to {@code message} for a key it does not know.</p>
 */
@Schema(
    name = "NotificationResponse",
    description = "Notification delivered to a platform account",
    example = "{\"id\": 12, \"recipient_user_id\": 42, \"equipment_id\": 10, \"device_id\": 7, "
        + "\"source_alert_id\": 301, \"service_request_id\": null, "
        + "\"message\": \"Cold Room #01 reached 9.4 °C, above its maximum of 8.0 °C\", "
        + "\"message_key\": \"alert.temperature_excursion.raised\", \"message_params\": {\"equipment_id\": 10, "
        + "\"equipment_name\": \"Cold Room #01\", \"temperature\": 9.4, \"limit\": \"MAX\", \"threshold\": 8.0}, "
        + "\"type\": \"TEMPERATURE_EXCURSION\", \"severity\": \"CRITICAL\", \"is_read\": false, "
        + "\"created_at\": \"2026-11-09T10:02:01Z\", \"read_at\": null, \"dismissed_at\": null}"
)
public record NotificationResource(
    @Schema(description = "Notification unique identifier", example = "12")
    Long id,

    @Schema(description = "Identifier of the owner or technician profile the notification is addressed to",
        example = "42")
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

    @Schema(description = "Identifier of the service request the notification is about; set for "
        + "SERVICE_REQUEST_UPDATE notifications and for MAINTENANCE_WINDOW notifications of a window linked "
        + "to a request, null otherwise", example = "55", nullable = true)
    Long serviceRequestId,

    @Schema(description = "Notification text in English; the fallback to show when message_key is unknown",
        example = "Cold Room #01 reached 9.4 °C, above its maximum of 8.0 °C")
    String message,

    @Schema(description = "Stable key of the message template, for client-side translation; never null. "
        + "notification.untemplated marks a notification stored before templates were recorded",
        example = "alert.temperature_excursion.raised",
        allowableValues = {"alert.temperature_excursion.raised", "alert.device_offline.raised",
            "alert.sensor_failure.raised", "service_request.assigned", "service_request.unassigned",
            "service_request.rejected", "service_request.canceled", "maintenance_window.opened",
            "maintenance_window.closed", "notification.untemplated"})
    String messageKey,

    @Schema(description = "Values filled into the template named by message_key; never null, possibly empty. "
        + "Values are strings, numbers or booleans, never markup",
        example = "{\"equipment_id\": 10, \"equipment_name\": \"Cold Room #01\", \"temperature\": 9.4, "
            + "\"limit\": \"MAX\", \"threshold\": 8.0}")
    Map<String, Object> messageParams,

    @Schema(description = "Kind of event that produced the notification", example = "TEMPERATURE_EXCURSION",
        allowableValues = {"MAINTENANCE_REMINDER", "TEMPERATURE_EXCURSION", "DEVICE_OFFLINE",
            "LOW_BATTERY", "SENSOR_FAILURE", "SERVICE_REQUEST_UPDATE", "MAINTENANCE_WINDOW",
            "UNLINKED_MAINTENANCE_WINDOW"})
    String type,

    @Schema(description = "Urgency of the notification",
        example = "CRITICAL", allowableValues = {"INFO", "WARNING", "CRITICAL"})
    String severity,

    @Schema(description = "Whether the user has already read the notification", example = "false")
    boolean isRead,

    @Schema(description = "When the notification was raised; never null. The inbox is ordered by it, most "
        + "recent first", example = "2026-11-09T10:02:01Z")
    Instant createdAt,

    @Schema(description = "When the notification was read; null while unread",
        example = "2026-10-23T17:00:00Z", nullable = true)
    Instant readAt,

    @Schema(description = "When the user dismissed the notification; null while not dismissed",
        example = "2026-10-23T17:05:00Z", nullable = true)
    Instant dismissedAt) {
}
