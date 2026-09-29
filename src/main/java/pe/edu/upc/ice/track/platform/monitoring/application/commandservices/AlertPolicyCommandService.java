package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;

/** Application service handling write operations on {@link AlertPolicy}. */
public interface AlertPolicyCommandService {

  AlertPolicy handle(UpdateAlertPolicyCommand command);
}
