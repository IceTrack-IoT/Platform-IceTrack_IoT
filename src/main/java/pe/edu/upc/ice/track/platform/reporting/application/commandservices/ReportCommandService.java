package pe.edu.upc.ice.track.platform.reporting.application.commandservices;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.RequestReportCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application command service for {@link Report}.
 *
 * <p>Implemented by {@code ReportCommandServiceImpl}, which validates the request and saves it in
 * {@code PENDING} status. The report is then generated in the background by
 * {@code ReportGenerationWorker}.</p>
 */
public interface ReportCommandService {

  /**
   *  Handles the command to request a new report on behalf of the authenticated caller: validates it
   *  and saves it as {@code PENDING}. Generation starts after the save has committed.
   * @param command the report request
   * @return the saved report, still pending; a 403 failure when the caller holds no owner profile; a
   *         404 failure when the requested site or unit does not belong to the caller; or a 400
   *         failure for an invalid request
   */
  Result<Report, ApplicationError> handle(RequestReportCommand command);
}
