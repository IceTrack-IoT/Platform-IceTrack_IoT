package pe.edu.upc.ice.track.platform.assets.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.assets.domain.model.aggregates.Equipment;

import java.util.List;

/**
 * One page of a listing, with the metadata a client needs to walk the rest of it.
 *
 * <p>A domain type on purpose. Paging is a property of how a large collection is read, not of any
 * framework, so the equipment query services return this rather than a framework page object and
 * keep the domain free of persistence abstractions.</p>
 *
 * <p>Requests that carry no {@code page}/{@code size} are answered with
 * {@link #unpaged(List)}, which reports itself as the single and only page so that a caller
 * cannot tell whether paging was requested.</p>
 *
 * @param content       the rows of this page
 * @param page          zero-based index of this page
 * @param size          maximum number of rows a page may hold
 * @param totalElements total number of rows across every page
 */
public record EquipmentPage(List<Equipment> content, int page, int size, long totalElements) {

  /**
   * Validates the page metadata.
   *
   * @throws IllegalArgumentException when the page index is negative, the size is not positive or
   *                                  the content is missing
   */
  public EquipmentPage {
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
   * Wraps a whole, unpaged collection as a single page.
   *
   * @param content every matching row
   * @return a page reporting itself as the only one
   */
  public static EquipmentPage unpaged(List<Equipment> content) {
    var size = Math.max(content.size(), 1);
    return new EquipmentPage(content, 0, size, content.size());
  }

  /**
   * Wraps a possibly truncated unpaged collection as a single page, reporting the true total.
   *
   * <p>The fetched {@code content} may be capped below {@code totalElements} when the owner's
   * matching rows exceed the unpaged listing's row cap; callers that need every row must then
   * switch to the paged form of the query.</p>
   *
   * @param content       the fetched rows, possibly truncated
   * @param totalElements the true number of rows matching the query, across the whole collection
   * @return a page reporting itself as the only one, with the real total
   */
  public static EquipmentPage unpaged(List<Equipment> content, long totalElements) {
    var size = Math.max(content.size(), 1);
    return new EquipmentPage(content, 0, size, totalElements);
  }

  /**
   * Computes the total number of pages.
   *
   * @return the page count, at least one so an empty listing still has a first page
   */
  public int totalPages() {
    return Math.max(1, (int) Math.ceil((double) totalElements / size));
  }

  /**
   * Tells whether this page carries no rows.
   *
   * @return {@code true} when the listing matched nothing
   */
  public boolean isEmpty() {
    return content.isEmpty();
  }
}