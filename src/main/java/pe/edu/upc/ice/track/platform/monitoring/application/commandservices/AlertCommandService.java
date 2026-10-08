package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;

import java.util.Optional;

/** Application service handling write operations on {@link Alert}. */
public interface AlertCommandService {

  /**
   *  Handles the command to acknowledge an alert.
   * @param command the command containing the alert identifier and the user acknowledging it
   * @return  an Optional containing the updated Alert if the operation was successful, or an empty Optional if the alert was not found or could not be acknowledged.
   */
  Optional<Alert> handle(AcknowledgeAlertCommand command);

  /**
   *  Handles the command to resolve an alert.
   * @param command the command containing the alert identifier and the user resolving it
   * @return  an Optional containing the updated Alert if the operation was successful, or an empty Optional if the alert was not found or could not be resolved.
   */
  Optional<Alert> handle(ResolveAlertCommand command);

  /**
   *  Handles the command to dismiss an alert.
   * @param command the command containing the alert identifier and the user dismissing it
   * @return  an Optional containing the updated Alert if the operation was successful, or an empty Optional if the alert was not found or could not be dismissed.
   */
  Optional<Alert> handle(DismissAlertCommand command);
}
