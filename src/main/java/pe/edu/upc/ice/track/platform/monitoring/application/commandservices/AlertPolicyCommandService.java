package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;

/** Application service handling write operations on {@link AlertPolicy}. */
public interface AlertPolicyCommandService {

  /**
   *  Updates an existing {@link AlertPolicy} based on the provided command.
   * @param command the command containing the update information
   * @return  the updated {@link AlertPolicy}
   */
  AlertPolicy handle(UpdateAlertPolicyCommand command);
}
