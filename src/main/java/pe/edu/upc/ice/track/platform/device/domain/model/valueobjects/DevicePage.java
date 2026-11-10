package pe.edu.upc.ice.track.platform.device.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.device.domain.model.aggregates.Device;

import java.util.List;

/**
 * One page of a device listing, with the metadata a client needs to walk the rest of it.
 *
 * <p>A domain type for the same reason as the paging types of the other contexts: paging is a
 * property of how a collection is read, not of a framework, so the repository port answers this
 * rather than a framework page object.</p>
 *
 * @param content       the devices of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of devices a page may hold
 * @param totalElements total number of devices across every page
 */
public record DevicePage(List<Device> content, int page, int size, long totalElements) {

  /**
   * Validates the page metadata.
   *
   * @throws IllegalArgumentException when the page index is negative, the size is not positive or
   *                                  the content is missing
   */
  public DevicePage {
    if (content == null) {
      throw new IllegalArgumentException("Page content must not be null");
    }
    if (page < 0) {
      throw new IllegalArgumentException("Page index must not be negative");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("Page size must be a positive number of rows");
    }
    content = List.copyOf(content);
  }

  /**
   * Builds a page that matched nothing.
   *
   * @param page zero-based index of the requested page
   * @param size size of the requested page
   * @return an empty page reporting a total of zero
   */
  public static DevicePage empty(int page, int size) {
    return new DevicePage(List.of(), page, size, 0L);
  }

  /**
   * Wraps a whole, unpaged collection as a single page.
   *
   * @param content every matching device
   * @return a page reporting itself as the only one
   */
  public static DevicePage unpaged(List<Device> content) {
    return new DevicePage(content, 0, Math.max(content.size(), 1), content.size());
  }

  /**
   * Computes the total number of pages.
   *
   * @return the page count, at least one so an empty listing still has a first page
   */
  public int totalPages() {
    return Math.max(1, (int) Math.ceil((double) totalElements / size));
  }
}
