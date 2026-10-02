package pe.edu.upc.ice.track.platform.assets.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of an equipment listing.
 *
 * <p>Returned instead of a bare array only when the caller asks for {@code page}/{@code size};
 * the common case still answers a plain array, so adding pagination did not change the shape a
 * non-paginating client already depends on.</p>
 *
 * @param content       the units of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of units a page may hold
 * @param totalElements total number of units matching the filters, across every page
 * @param totalPages    number of pages the matching units span
 */
@Schema(
    name = "PagedEquipmentResponse",
    description = "One page of an equipment listing",
    example = "{\"content\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedEquipmentResource(
    @Schema(description = "Equipment units of this page")
    List<EquipmentResource> content,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Maximum number of units a page may hold", example = "20")
    int size,

    @Schema(description = "Total number of units matching the filters", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching units span", example = "1")
    int totalPages
) {
}