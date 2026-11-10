package pe.edu.upc.ice.track.platform.reporting.domain.model.queries;

import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportListFilter;

/**
 * Query to list every report the authenticated caller has requested that matches a filter, most
 * recent first.
 *
 * @param username username of the authenticated caller; required
 * @param filter   name, type, status and request-date restrictions; {@code null} restricts nothing
 */
public record GetReportsByUserQuery(String username, ReportListFilter filter) {

  /** Normalizes a missing filter to one that restricts nothing. */
  public GetReportsByUserQuery {
    filter = filter == null ? ReportListFilter.NONE : filter;
  }
}
