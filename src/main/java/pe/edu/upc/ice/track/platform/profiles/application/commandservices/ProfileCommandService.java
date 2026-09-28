package pe.edu.upc.ice.track.platform.profiles.application.commandservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateOwnerProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateTechnicianProfileCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Profile Command Service
 */
public interface ProfileCommandService {

  /**
   * Handle Create Owner Profile Command.
   *
   * <p>Instantiates an {@code OwnerProfile} through the {@code OwnerProfileFactory} and persists
   * it.</p>
   *
   * @param command The {@link CreateOwnerProfileCommand} Command
   * @return A {@link Result} containing the created {@link Profile} on success,
   *         or an {@link ApplicationError} on failure (validation or conflict)
   */
  Result<Profile, ApplicationError> handle(CreateOwnerProfileCommand command);

  /**
   * Handle Create Technician Profile Command.
   *
   * <p>Instantiates a {@code TechnicianProfile} through the {@code TechnicianProfileFactory} and
   * persists it.</p>
   *
   * @param command The {@link CreateTechnicianProfileCommand} Command
   * @return A {@link Result} containing the created {@link Profile} on success,
   *         or an {@link ApplicationError} on failure (validation or conflict)
   */
  Result<Profile, ApplicationError> handle(CreateTechnicianProfileCommand command);
}
