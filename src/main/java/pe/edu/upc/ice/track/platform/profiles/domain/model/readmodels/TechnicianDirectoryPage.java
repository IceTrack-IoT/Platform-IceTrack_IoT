package pe.edu.upc.ice.track.platform.profiles.domain.model.readmodels;

import java.util.List;

/**
 * One page of the public technician directory, with the metadata a client needs to walk the rest of
 * it.
 *
 * @param content       the technicians of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of technicians a page may hold
 * @param totalElements total number of technicians matching the filters, across every page
 */
public record TechnicianDirectoryPage(List<TechnicianDirectoryEntry> content, int page, int size, long totalElements) {

  /**
   * Validates the page metadata.
   *
   * @throws IllegalArgumentException when the page index is negative, the size is not positive or
   *                                  the content is missing
   */
  public TechnicianDirectoryPage {
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
   * Computes the total number of pages.
   *
   * @return the page count, at least one so an empty directory still has a first page
   */
  public int totalPages() {
    return Math.max(1, (int) Math.ceil((double) totalElements / size));
  }
}
