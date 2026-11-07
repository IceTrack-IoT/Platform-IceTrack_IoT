package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Application service handling write operations on {@link Alert}.
 *
 * <p>Every transition records the authenticated caller as its actor. Failures are:</p>
 * <ul>
 *   <li>{@code ALERT_NOT_FOUND} - the alert does not exist, or the caller does not own its unit;</li>
 *   <li>{@code INVALID_STATUS_TRANSITION} - the alert's current status does not allow the transition;</li>
 *   <li>{@code VALIDATION_ERROR} - a dismissal reason or evidence is rejected.</li>
 * </ul>
 */
public interface AlertCommandService {

  /**
   * Handles the command to acknowledge an OPEN alert.
   *
   * @param command the command containing the alert identifier and the user acknowledging it
   * @return the updated alert, or the reason it could not be acknowledged
   */
  Result<Alert, ApplicationError> handle(AcknowledgeAlertCommand command);

  /**
   * Handles the command to resolve an OPEN or ACKNOWLEDGED alert.
   *
   * @param command the command containing the alert identifier and the user resolving it
   * @return the updated alert, or the reason it could not be resolved
   */
  Result<Alert, ApplicationError> handle(ResolveAlertCommand command);

  /**
   * Handles the command to dismiss an OPEN or ACKNOWLEDGED alert.
   *
   * @param command the command containing the alert identifier, the user dismissing it and why
   * @return the updated alert, or the reason it could not be dismissed
   */
  Result<Alert, ApplicationError> handle(DismissAlertCommand command);
}
