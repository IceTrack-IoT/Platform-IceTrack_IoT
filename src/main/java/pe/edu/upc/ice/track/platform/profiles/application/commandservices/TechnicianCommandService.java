package pe.edu.upc.ice.track.platform.profiles.application.commandservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.TechnicianProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateTechnicianCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Technician Command Service
 */
public interface TechnicianCommandService {

  /**
   * Handle Create Technician Command.
   *
   * @param command The {@link CreateTechnicianCommand} Command
   * @return A {@link Result} containing the created {@link TechnicianProfile} on success,
   *         or an {@link ApplicationError} on failure (validation or conflict)
   */
  Result<TechnicianProfile, ApplicationError> handle(CreateTechnicianCommand command);

  /**
   * Handle Update Technician Command.
   *
   * <p>Loads the technician profile, applies the changes through its domain methods and persists it.</p>
   *
   * @param command The {@link UpdateTechnicianCommand} Command
   * @return A {@link Result} containing the updated {@link TechnicianProfile} on success,
   *         or an {@link ApplicationError} when the technician is missing or a value is invalid
   */
  Result<TechnicianProfile, ApplicationError> handle(UpdateTechnicianCommand command);
}
