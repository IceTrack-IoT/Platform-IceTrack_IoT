package pe.edu.upc.ice.track.platform.notifications.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of the caller's notification inbox.
 *
 * @param items         the notifications of this page, most recent first
 * @param page          zero-based index of this page
 * @param size          maximum number of notifications a page may hold
 * @param totalElements total number of notifications matching the filter, across every page
 * @param totalPages    number of pages the matching notifications span
 */
@Schema(
    name = "PagedNotificationResponse",
    description = "One page of the caller's active notifications",
    example = "{\"items\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedNotificationResource(
    @Schema(description = "Notifications of this page, most recent first")
    List<NotificationResource> items,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Maximum number of notifications a page may hold", example = "20")
    int size,

    @Schema(description = "Total number of notifications matching the filter", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching notifications span; at least 1", example = "1")
    int totalPages
) {
}
