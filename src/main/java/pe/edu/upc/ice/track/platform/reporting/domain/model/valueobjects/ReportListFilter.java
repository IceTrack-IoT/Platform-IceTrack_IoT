package pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects;

import java.time.Instant;
import java.util.Set;

/**
 * Optional restrictions on which of a user's reports a listing returns.
 *
 * <p>The requesting user is never part of the filter, so no combination of values can reach
 * another user's reports: every restriction only narrows the caller's own history.</p>
 *
 * @param search        case-insensitive fragment the report name must contain; {@code null} or
 *                      blank does not restrict; at most {@value #MAX_SEARCH_LENGTH} characters
 * @param types         restrict to these report types; {@code null} or empty does not restrict
 * @param statuses      restrict to these statuses; {@code null} or empty does not restrict
 * @param requestedFrom inclusive lower bound of the request instant; {@code null} does not restrict
 * @param requestedTo   inclusive upper bound of the request instant; {@code null} does not restrict
 */
public record ReportListFilter(
    String search, Set<ReportType> types, Set<ReportStatus> statuses, Instant requestedFrom, Instant requestedTo) {

  /** Longest search text accepted, the width of the report name. */
  public static final int MAX_SEARCH_LENGTH = 150;

  /** A filter that restricts nothing. */
  public static final ReportListFilter NONE = new ReportListFilter(null, Set.of(), Set.of(), null, null);

  /**
   * Normalizes the values and validates them.
   *
   * @throws IllegalArgumentException when the search text is too long or the request range is
   *                                  inverted
   */
  public ReportListFilter {
    search = search == null || search.isBlank() ? null : search.strip();
    if (search != null && search.length() > MAX_SEARCH_LENGTH) {
      throw new IllegalArgumentException(
          "Parameter 'search' must not exceed %d characters".formatted(MAX_SEARCH_LENGTH));
    }
    types = types == null ? Set.of() : Set.copyOf(types);
    statuses = statuses == null ? Set.of() : Set.copyOf(statuses);
    if (requestedFrom != null && requestedTo != null && requestedFrom.isAfter(requestedTo)) {
      throw new IllegalArgumentException("Parameter 'requested_from' must not be after 'requested_to'");
    }
  }
}
