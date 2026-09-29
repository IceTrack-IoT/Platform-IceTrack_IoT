package pe.edu.upc.ice.track.platform.profiles.application.commandservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.OwnerProfile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.UpdateOwnerCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Owner Command Service
 */
public interface OwnerCommandService {

  /**
   * Handle Create Owner Command.
   *
   * @param command The {@link CreateOwnerCommand} Command
   * @return A {@link Result} containing the created {@link OwnerProfile} on success,
   *         or an {@link ApplicationError} on failure (validation or conflict)
   */
  Result<OwnerProfile, ApplicationError> handle(CreateOwnerCommand command);

  /**
   * Handle Update Owner Command.
   *
   * <p>Loads the owner profile, applies the changes through its domain methods and persists it.</p>
   *
   * @param command The {@link UpdateOwnerCommand} Command
   * @return A {@link Result} containing the updated {@link OwnerProfile} on success,
   *         or an {@link ApplicationError} when the owner is missing or a value is invalid
   */
  Result<OwnerProfile, ApplicationError> handle(UpdateOwnerCommand command);
}
