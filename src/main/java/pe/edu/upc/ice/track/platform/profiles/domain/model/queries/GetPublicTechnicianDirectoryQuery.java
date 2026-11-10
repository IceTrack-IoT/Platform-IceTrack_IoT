package pe.edu.upc.ice.track.platform.profiles.domain.model.queries;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.TechnicianDirectorySort;

/**
 * Query for one page of the public technician directory: what an owner may see of every technician
 * when choosing whom to assign work to, with their projected statistics.
 *
 * <p>Always paged. A page size above {@link #MAX_PAGE_SIZE} is capped to it rather than refused.</p>
 *
 * @param search     case-insensitive text matched against the full name or the speciality, may be
 *                   {@code null}
 * @param speciality restrict to technicians of exactly this speciality, whatever its case, may be
 *                   {@code null}
 * @param minRating  restrict to technicians rated at least this much, from 0 to 5; technicians not
 *                   rated yet are then left out; may be {@code null}
 * @param sort       order of the directory; {@code null} is read as {@link TechnicianDirectorySort#NAME}
 * @param page       zero-based page index; not negative
 * @param size       page size; at least one
 */
public record GetPublicTechnicianDirectoryQuery(
    String search,
    String speciality,
    Double minRating,
    TechnicianDirectorySort sort,
    int page,
    int size) {

  /** Largest page a client is served. */
  public static final int MAX_PAGE_SIZE = 100;

  /** Highest rating a technician can hold. */
  public static final double MAX_RATING = 5.0;

  /**
   * Normalizes the filters and validates the rating bound and the paging values.
   *
   * @throws IllegalArgumentException when the rating bound is outside 0 to 5, the page index is
   *                                  negative or the page size is below one
   */
  public GetPublicTechnicianDirectoryQuery {
    search = search == null || search.isBlank() ? null : search.strip();
    speciality = speciality == null || speciality.isBlank() ? null : speciality.strip();
    sort = sort == null ? TechnicianDirectorySort.NAME : sort;
    if (minRating != null && (minRating.isNaN() || minRating < 0 || minRating > MAX_RATING)) {
      throw new IllegalArgumentException("Parameter 'min_rating' must be between 0 and 5");
    }
    if (page < 0) {
      throw new IllegalArgumentException("Parameter 'page' must not be negative");
    }
    if (size < 1) {
      throw new IllegalArgumentException("Parameter 'size' must be at least 1");
    }
    if (size > MAX_PAGE_SIZE) {
      size = MAX_PAGE_SIZE;
    }
  }
}
