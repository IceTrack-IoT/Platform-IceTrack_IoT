package pe.edu.upc.ice.track.platform.reporting.application.queryservices;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportPageByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportPage;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

import java.util.List;
import java.util.Optional;

/**
 * Application query service for {@link Report}.
 *
 * <p>Implemented by {@code ReportQueryServiceImpl}, which delegates directly to
 * {@code ReportRepository}.</p>
 */
public interface ReportQueryService {

  /**
   *  Handles the query to retrieve a report by its identifier.
   * @param query the query containing the report identifier
   * @return an {@code Optional} containing the report if found, or empty if not found
   */
  Optional<Report> handle(GetReportByIdQuery query);

  /**
   *  Handles the query to list every report a user has requested that matches a filter.
   * @param query the query containing the requesting user and the filter
   * @return the matching reports, most recent first; empty when there are none
   */
  List<Report> handle(GetReportsByUserQuery query);

  /**
   *  Handles the query to read one page of the reports a user has requested that match a filter.
   * @param query the query containing the requesting user, the filter and the page
   * @return the requested page, empty when the caller has no account
   */
  ReportPage handle(GetReportPageByUserQuery query);

  /**
   *  Handles the query to obtain a temporary download link for a report's file.
   * @param query the query containing the report identifier and the asking user
   * @return the link; {@code REPORT_NOT_FOUND} when the report does not exist or belongs to another
   *         user, or {@code INVALID_STATUS_TRANSITION} when it is not {@code COMPLETED}
   */
  Result<ReportDownloadLink, ApplicationError> handle(GetReportDownloadLinkQuery query);
}
