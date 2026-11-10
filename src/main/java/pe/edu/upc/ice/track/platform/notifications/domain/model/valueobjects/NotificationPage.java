package pe.edu.upc.ice.track.platform.notifications.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.notifications.domain.model.aggregates.Notification;

import java.util.List;

/**
 * One page of a recipient's notification inbox, with the metadata a client needs to walk the
 * rest of it.
 *
 * <p>A domain type on purpose, like the paging types of the other contexts: paging is a property
 * of how a large collection is read, not of any framework, so the query service returns this
 * rather than a framework page object.</p>
 *
 * @param content       the notifications of this page, most recent first
 * @param page          zero-based index of this page
 * @param size          maximum number of notifications a page may hold
 * @param totalElements total number of notifications across every page
 */
public record NotificationPage(List<Notification> content, int page, int size, long totalElements) {

  /**
   * Validates the page metadata.
   *
   * @throws IllegalArgumentException when the page index is negative, the size is not positive or
   *                                  the content is missing
   */
  public NotificationPage {
    if (content == null) {
      throw new IllegalArgumentException("Page content must not be null");
    }
    if (page < 0) {
      throw new IllegalArgumentException("Page index must not be negative");
    }
    if (size <= 0) {
      throw new IllegalArgumentException("Page size must be a positive number of notifications");
    }
    content = List.copyOf(content);
  }

  /**
   * Builds an empty page, for a caller who can receive no notifications.
   *
   * @param page zero-based index of the requested page
   * @param size requested page size
   * @return a page with no notifications and a total of zero
   */
  public static NotificationPage empty(int page, int size) {
    return new NotificationPage(List.of(), page, size, 0L);
  }

  /**
   * Computes the total number of pages.
   *
   * @return the page count, at least one so an empty inbox still has a first page
   */
  public int totalPages() {
    return Math.max(1, (int) Math.ceil((double) totalElements / size));
  }
}
