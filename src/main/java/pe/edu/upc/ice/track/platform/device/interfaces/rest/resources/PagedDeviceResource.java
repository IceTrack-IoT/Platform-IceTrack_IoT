package pe.edu.upc.ice.track.platform.device.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Outbound REST resource wrapping one page of a device listing.
 *
 * <p>Returned instead of a bare array only when the caller asks for {@code page} and {@code size}.
 * Same shape as the paged equipment and service request listings, so a client walks them all the
 * same way.</p>
 *
 * @param content       the devices of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of devices a page may hold
 * @param totalElements total number of devices matching the filters, across every page
 * @param totalPages    number of pages the matching devices span
 */
@Schema(
    name = "PagedDeviceResponse",
    description = "One page of a device listing",
    example = "{\"content\": [], \"page\": 0, \"size\": 20, \"total_elements\": 0, \"total_pages\": 1}")
public record PagedDeviceResource(
    @Schema(description = "Devices of this page, ordered by device_uid")
    List<DeviceResource> content,

    @Schema(description = "Zero-based index of this page", example = "0")
    int page,

    @Schema(description = "Maximum number of devices a page may hold", example = "20")
    int size,

    @Schema(description = "Total number of devices matching the filters", example = "0")
    long totalElements,

    @Schema(description = "Number of pages the matching devices span; at least 1", example = "1")
    int totalPages
) {
}
