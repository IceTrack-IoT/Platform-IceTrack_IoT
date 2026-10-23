package pe.edu.upc.ice.track.platform.service.request.application.commandservices;

import pe.edu.upc.ice.track.platform.service.request.domain.model.aggregates.ServiceRequest;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AcceptServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.AssignTechnicianCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CancelServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.CreateServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RegisterInterventionCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.RejectServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.commands.StartServiceRequestCommand;
import pe.edu.upc.ice.track.platform.service.request.domain.model.valueobjects.InterventionRegistration;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service handling write operations on {@link ServiceRequest}.
 *
 * <p>Every command names its caller by username. Owner commands only reach the requests the caller
 * created, technician commands only the requests assigned to the caller; any other request is
 * reported exactly like one that does not exist.</p>
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
   * Accept a request assigned to the caller.
   *
   * @param command The {@link AcceptServiceRequestCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the request is not assigned to the caller or is not
   *         {@code PENDING}
   */
  Result<ServiceRequest, ApplicationError> handle(AcceptServiceRequestCommand command);

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
   * Register the intervention that completes a request assigned to the caller.
   *
   * @param command The {@link RegisterInterventionCommand} Command
   * @return A {@link Result} containing the {@link InterventionRegistration}, which tells a new
   *         registration from a replay, or an {@link ApplicationError} when the request is not
   *         assigned to the caller, is not {@code IN_PROGRESS}, the client identifier belongs to
   *         another request, or the report is invalid
   */
  Result<InterventionRegistration, ApplicationError> handle(RegisterInterventionCommand command);

  /**
   * Cancel one of the caller's requests before the work starts.
   *
   * @param command The {@link CancelServiceRequestCommand} Command
   * @return A {@link Result} containing the updated {@link ServiceRequest}, or an
   *         {@link ApplicationError} when the request is not the caller's, the work already started,
   *         or no reason is given
   */
  Result<ServiceRequest, ApplicationError> handle(CancelServiceRequestCommand command);
}
