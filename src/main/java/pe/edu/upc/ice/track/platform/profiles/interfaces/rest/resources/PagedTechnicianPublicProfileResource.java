package pe.edu.upc.ice.track.platform.profiles.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of the public technician directory.
 *
 * <p>Same shape as the paged equipment and service request listings, so a client walks them all the
 * same way. Like every {@link TechnicianPublicProfileResource}, it carries no personal data.</p>
 *
 * @param content       the technicians of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of technicians a page may hold
 * @param totalElements total number of technicians matching the filters, across every page
 * @param totalPages    number of pages the matching technicians span
 */
@Schema(
    name = "PagedTechnicianPublicProfileResponse",
    description = "One page of the public technician directory",
    example = "{\"content\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedTechnicianPublicProfileResource(
    @Schema(description = "Technicians of this page")
    List<TechnicianPublicProfileResource> content,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Maximum number of technicians a page may hold", example = "20")
    int size,

    @Schema(description = "Total number of technicians matching the filters", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching technicians span; at least 1", example = "1")
    int totalPages
) {
}
