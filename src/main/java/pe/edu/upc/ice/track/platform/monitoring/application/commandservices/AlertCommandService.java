package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.AcknowledgeAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.DismissAlertCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.ResolveAlertCommand;

import java.util.Optional;

/** Application service handling write operations on {@link Alert}. */
public interface AlertCommandService {

  Optional<Alert> handle(AcknowledgeAlertCommand command);

  Optional<Alert> handle(ResolveAlertCommand command);

  Optional<Alert> handle(DismissAlertCommand command);
}
