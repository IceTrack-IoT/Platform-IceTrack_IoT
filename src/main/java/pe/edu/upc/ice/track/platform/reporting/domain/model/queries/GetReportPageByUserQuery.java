package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;

/**
 * Query to read one page of the reports the authenticated caller has requested that match a
 * filter, most recent first.
 *
 * <p>The page size is capped at {@value #MAX_PAGE_SIZE}: a larger value is lowered to the cap
 * rather than rejected.</p>
 *
 * @param username username of the authenticated caller; required
 * @param filter   name, type, status and request-date restrictions; {@code null} restricts nothing
 * @param page     zero-based page index; must not be negative
 * @param size     page size; must be positive, capped at {@value #MAX_PAGE_SIZE}
 */
public record GetReportPageByUserQuery(String username, ReportListFilter filter, int page, int size) {

  /** Largest page size served. */
  public static final int MAX_PAGE_SIZE = 100;

  /**
   * Validates the paging values, normalizes the filter and caps the page size.
   *
   * @throws IllegalArgumentException when the page index is negative or the page size is not
   *                                  positive
   */
  public GetReportPageByUserQuery {
    if (page < 0) {
      throw new IllegalArgumentException("Parameter 'page' must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("Parameter 'size' must be at least 1");
    }
    filter = filter == null ? ReportListFilter.NONE : filter;
    size = Math.min(size, MAX_PAGE_SIZE);
  }
}
