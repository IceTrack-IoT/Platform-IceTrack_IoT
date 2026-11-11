package pe.edu.upc.ice.track.platform.reporting.application.commandservices;

import pe.edu.upc.ice.track.platform.reporting.domain.model.aggregates.Report;
import pe.edu.upc.ice.track.platform.reporting.domain.model.commands.DeleteReportCommand;
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

  /**
   *  Handles the command to delete one of the caller's finished reports. Its stored file is removed
   *  once the deletion has committed.
   * @param command the report to delete and the caller
   * @return the identifier of the deleted report; a 404 {@code REPORT_NOT_FOUND} failure when the report
   *         does not exist or belongs to somebody else; or a 409 {@code INVALID_STATUS_TRANSITION}
   *         failure while it is {@code PENDING} or {@code GENERATING}
   */
  Result<Long, ApplicationError> handle(DeleteReportCommand command);
}
