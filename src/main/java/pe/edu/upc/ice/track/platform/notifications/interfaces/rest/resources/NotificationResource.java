package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import java.time.LocalDateTime;

/** Outbound REST resource representing a notification. */
public record NotificationResource(
    Long id,
    Long equipmentId,
    Long deviceId,
    Long sourceAlertId,
    String message,
    String type,
    String severity,
    boolean isRead,
    LocalDateTime readAt,
    LocalDateTime dismissedAt) {
}
