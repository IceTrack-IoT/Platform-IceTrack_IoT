package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of an alert listing.
 *
 * <p>Same shape as the paged equipment and service request listings, so a client walks them all the
 * same way.</p>
 *
 * @param content       the alerts of this page, most recently opened first
 * @param page          zero-based index of this page
 * @param size          maximum number of alerts a page may hold
 * @param totalElements total number of alerts matching the filters, across every page
 * @param totalPages    number of pages the matching alerts span
 */
@Schema(
    name = "PagedAlertResponse",
    description = "One page of an alert listing",
    example = "{\"content\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedAlertResource(
    @Schema(description = "Alerts of this page, most recently opened first")
    List<AlertResource> content,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Maximum number of alerts a page may hold", example = "20")
    int size,

    @Schema(description = "Total number of alerts matching the filters", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching alerts span; at least 1", example = "1")
    int totalPages
) {
}
