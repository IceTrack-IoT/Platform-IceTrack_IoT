package pe.edu.upc.ice.track.platform.device.application.results;

import java.util.List;

/**
 * One page of the device inventory, each board described with the unit it is linked to.
 *
 * @param content       the boards of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of boards a page may hold
 * @param totalElements total number of boards matching the filters, across every page
 */
public record DeviceDetailsPage(List<DeviceDetails> content, int page, int size, long totalElements) {

  /**
   * Keeps the content unmodifiable.
   */
  public DeviceDetailsPage {
    content = List.copyOf(content);
  }

  /**
   * Computes the total number of pages.
   *
   * @return the page count, at least one so an empty inventory still has a first page
   */
  public int totalPages() {
    return Math.max(1, (int) Math.ceil((double) totalElements / size));
  }
}
