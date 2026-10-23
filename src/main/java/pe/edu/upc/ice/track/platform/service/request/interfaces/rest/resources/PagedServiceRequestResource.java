package pe.edu.upc.ice.track.platform.service.request.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of a service request listing.
 *
 * <p>Same shape as {@code PagedEquipmentResource}, so a client walks both listings the same way.</p>
 *
 * @param content       the requests of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of requests a page may hold
 * @param totalElements total number of requests matching the filters, across every page
 * @param totalPages    number of pages the matching requests span
 */
@Schema(
    name = "PagedServiceRequestResponse",
    description = "One page of a service request listing",
    example = "{\"content\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedServiceRequestResource(
    @Schema(description = "Service requests of this page, newest first")
    List<ServiceRequestResource> content,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Maximum number of requests a page may hold", example = "20")
    int size,

    @Schema(description = "Total number of requests matching the filters", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching requests span", example = "1")
    int totalPages
) {
}
