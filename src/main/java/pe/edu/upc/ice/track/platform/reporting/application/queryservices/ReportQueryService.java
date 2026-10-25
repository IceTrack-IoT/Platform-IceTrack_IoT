package pe.edu.upc.ice.track.platform.reporting.application.queryservices;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportByIdQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportDownloadLinkQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.queries.GetReportsByUserQuery;
import pe.edu.upc.ice.track.platform.reporting.domain.model.valueobjects.ReportDownloadLink;

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
   *  Handles the query to list every report a user has requested.
   * @param query the query containing the requesting user's identifier
   * @return the user's reports, empty when they have none
   */
  List<Report> handle(GetReportsByUserQuery query);
  /**
   *  Handles the query to obtain a temporary download link for a report's file.
   * @param query the query containing the report identifier and the asking user
   * @return an {@code Optional} containing the link, or empty when the report does not exist,
   *         belongs to another user, or has no file (it is not {@code COMPLETED})
   */
  Optional<ReportDownloadLink> handle(GetReportDownloadLinkQuery query);
}
