package pe.edu.upc.ice.track.platform.monitoring.application.commandservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.SeedDefaultAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.commands.UpdateAlertPolicyCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/** Application service handling write operations on {@link AlertPolicy}. */
public interface AlertPolicyCommandService {

  /**
   * Creates or updates the alert policy of one of the caller's units.
   *
   * @param command the command containing the update information
   * @return the created or updated {@link AlertPolicy}; a 403 failure when no equipment is named
   *         (the platform default is not editable); a 404 failure when the caller does not own the
   *         equipment; or a 400 failure when a value is out of bounds
   */
  Result<AlertPolicy, ApplicationError> handle(UpdateAlertPolicyCommand command);

  /**
   * Stores the platform-wide default policy when none exists yet. Idempotent.
   *
   * @param command the seeding command
   */
  void handle(SeedDefaultAlertPolicyCommand command);
}
