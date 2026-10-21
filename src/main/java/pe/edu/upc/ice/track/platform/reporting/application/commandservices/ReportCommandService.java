package pe.edu.upc.ice.track.platform.reporting.application.commandservices;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;

/**
 * Application command service for {@link Report}.
 *
 * <p>Implemented by {@code ReportCommandServiceImpl}, which resolves and runs the matching
 * {@code ReportGenerationStrategy} synchronously within the same request, then persists the
 * resulting report once.</p>
 */
public interface ReportCommandService {

  /**
   *  Handles the command to request a new report: builds it, generates its KPIs through the
   *  matching strategy, and persists it in its final state (completed or failed).
   * @param command the report request
   * @return the persisted report
   */
  Report handle(RequestReportCommand command);
}
