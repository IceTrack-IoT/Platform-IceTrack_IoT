package pe.edu.upc.ice.track.platform.service.request.application.commandservices;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AcceptServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AssignTechnicianCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CancelServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CompleteMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CreateServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RecordMaintenanceWindowLinkCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegenerateEdgeGrantCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegisterInterventionCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RejectServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.StartServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.AcceptedServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.InterventionRegistration;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.IssuedEdgeGrant;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service handling write operations on {@link ServiceRequest}.
 *
 * <p>Every REST command names its caller by username. Owner commands only reach the requests the
 * caller created, technician commands only the requests assigned to the caller; any other request is
 * reported exactly like one that does not exist. The maintenance window link commands are the
 * exception: they come from Monitoring and Alerting's events, not from a caller.</p>
 */
public interface ServiceRequestCommandService {

  /**
   * Create a service request for one of the caller's units.
   *
   * @param command The {@link CreateServiceRequestCommand} Command
   * @return A {@link Result} containing the created {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the caller is not an owner, the unit is not one of its
   *         own, or a value is invalid
   */
  Result<ServiceRequest, ApplicationError> handle(CreateServiceRequestCommand command);

  /**
   * Assign a technician to one of the caller's requests.
   *
   * @param command The {@link AssignTechnicianCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the request is not the caller's, the technician is not
   *         eligible, or the status does not allow an assignment
   */
  Result<ServiceRequest, ApplicationError> handle(AssignTechnicianCommand command);

  /**
   * Accept a request assigned to the caller, issuing an edge grant for the unit's gateway in the
   * same transaction.
   *
   * @param command The {@link AcceptServiceRequestCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest} together with the issued
   *         grant and its plaintext token, or an {@link ApplicationError} when the request is not
   *         assigned to the caller or is not {@code PENDING}
   */
  Result<AcceptedServiceRequest, ApplicationError> handle(AcceptServiceRequestCommand command);

  /**
   * Replace the edge grant of a request assigned to the caller, revoking the previous one.
   *
   * @param command The {@link RegenerateEdgeGrantCommand} Command
   * @return A {@link Result} containing the new grant and its plaintext token, or an
   *         {@link ApplicationError}: 404 when the request does not exist, 403 when it is assigned to
   *         another technician, 409 when it is neither {@code ACCEPTED} nor {@code IN_PROGRESS}
   */
  Result<IssuedEdgeGrant, ApplicationError> handle(RegenerateEdgeGrantCommand command);

  /**
   * Reject a request assigned to the caller.
   *
   * @param command The {@link RejectServiceRequestCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the request is not assigned to the caller, is not
   *         {@code PENDING}, or no reason is given
   */
  Result<ServiceRequest, ApplicationError> handle(RejectServiceRequestCommand command);

  /**
   * Start the work on a request assigned to the caller.
   *
   * @param command The {@link StartServiceRequestCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the request is not assigned to the caller or is not
   *         {@code ACCEPTED}
   */
  Result<ServiceRequest, ApplicationError> handle(StartServiceRequestCommand command);

  /**
   * Register the intervention that completes a request assigned to the caller. A new registration
   * revokes the request's edge grants.
   *
   * @param command The {@link RegisterInterventionCommand} Command
   * @return A {@link Result} containing the {@link InterventionRegistration}, which tells a new
   *         registration from a replay, or an {@link ApplicationError} when the request is not
   *         assigned to the caller, is not {@code IN_PROGRESS}, the client identifier belongs to
   *         another request, or the report is invalid
   */
  Result<InterventionRegistration, ApplicationError> handle(RegisterInterventionCommand command);

  /**
   * Cancel one of the caller's requests before the work starts, revoking its edge grants.
   *
   * @param command The {@link CancelServiceRequestCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the request is not the caller's, the work already started,
   *         or no reason is given
   */
  Result<ServiceRequest, ApplicationError> handle(CancelServiceRequestCommand command);

  /**
   * Record on a request a maintenance window opened on its unit while the work was in progress.
   *
   * <p>Not caller-scoped: issued by an inbound adapter, in a transaction of its own. A request that
   * does not exist, belongs to another unit, was not in progress when the window was attributed to
   * it, or already holds the window, is left unchanged and the inconsistency is logged; nothing is
   * thrown for those cases, so the event is acknowledged without retry.</p>
   *
   * @param command The {@link RecordMaintenanceWindowLinkCommand} Command
   * @return {@code true} when a link was added
   */
  boolean handle(RecordMaintenanceWindowLinkCommand command);

  /**
   * Complete the link of a closed maintenance window with its end and closure reason.
   *
   * <p>Not caller-scoped and in a transaction of its own, like the recording above. A window that is
   * not linked to the request, or whose link is already complete, changes nothing.</p>
   *
   * @param command The {@link CompleteMaintenanceWindowLinkCommand} Command
   * @return {@code true} when a link was completed
   */
  boolean handle(CompleteMaintenanceWindowLinkCommand command);
}
