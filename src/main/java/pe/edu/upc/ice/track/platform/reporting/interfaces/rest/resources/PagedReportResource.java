package pe.edu.upc.ice.track.platform.reporting.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of the caller's report history.
 *
 * <p>Same shape as the other paged listings, so a client walks them all the same way. Its items are
 * summaries: they carry neither {@code result_payload}, {@code kpis} nor {@code download_url}.</p>
 *
 * @param content       the reports of this page
 * @param page          zero-based index of this page
 * @param size          page size after capping
 * @param totalElements number of reports matching the filters, across every page
 * @param totalPages    number of pages the matching reports span, at least 1
 */
@Schema(
    name = "PagedReportResponse",
    description = "One page of the caller's report history",
    example = "{\"content\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedReportResource(
    @Schema(description = "Reports of this page, most recently requested first")
    List<ReportResource> content,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Page size after capping", example = "20")
    int size,

    @Schema(description = "Number of reports matching the filters", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching reports span, at least 1", example = "1")
    int totalPages
) {
}
