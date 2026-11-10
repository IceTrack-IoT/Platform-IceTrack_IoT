package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Data of the {@code unread-count-changed} stream event: the stream owner's unread notification count
 * changed (a notification arrived, or one or several were read or dismissed).
 */
@Schema(
    name = "UnreadCountChangedStreamEvent",
    description = "Data of the unread-count-changed event",
    example = "{\"unread_count\": 4, \"changed_at\": \"2026-11-09T10:02:01Z\"}")
public record UnreadCountChangedStreamResource(
    @Schema(description = "Unread count after the change", example = "4")
    long unreadCount,

    @Schema(description = "When it changed", example = "2026-11-09T10:02:01Z")
    Instant changedAt
) {
}
