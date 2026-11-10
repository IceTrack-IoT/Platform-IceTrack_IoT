package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;

import java.util.List;

/**
 * One page of an alert listing, with the metadata a client needs to walk the rest of it.
 *
 * <p>A domain type for the same reason as the paging types of the other contexts: paging is a
 * property of how a collection is read, not of a framework, so the repository port answers this
 * rather than a framework page object.</p>
 *
 * @param content       the alerts of this page, most recently opened first
 * @param page          zero-based index of this page
 * @param size          maximum number of alerts a page may hold
 * @param totalElements total number of alerts across every page
 */
public record AlertPage(List<Alert> content, int page, int size, long totalElements) {

  /**
   * Validates the page metadata.
   *
   * @throws IllegalArgumentException when the page index is negative, the size is not positive or
   *                                  the content is missing
   */
  public AlertPage {
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
  public static AlertPage empty(int page, int size) {
    return new AlertPage(List.of(), page, size, 0L);
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
