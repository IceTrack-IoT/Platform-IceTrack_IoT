package pe.edu.upc.ice.track.platform.profiles.application.commandservices;

import pe.edu.upc.ice.track.platform.profiles.domain.model.aggregates.Profile;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.CreateUserProfileCommand;
import pe.edu.upc.ice.track.platform.profiles.domain.model.commands.LinkProfileToUserCommand;
import pe.edu.upc.ice.track.platform.shared.application.result.ApplicationError;
import pe.edu.upc.ice.track.platform.shared.application.result.Result;

/**
 * Profile Command Service
 */
public interface ProfileCommandService {
  /**
   * Handle Create Profile Command
   *
   * @param command The {@link CreateProfileCommand} Command
   * @return A {@link Result} containing the created {@link Profile} on success,
   *         or an {@link ApplicationError} on failure (validation or business rule violation)
   */
  Result<Profile, ApplicationError> handle(CreateProfileCommand command);

  /**
   * Handle Create User Profile Command.
   *
   * <p>Instantiates the profile of a newly registered platform account by delegating to the
   * {@code UserProfileFactory} matching the role carried by the creation data.</p>
   *
   * @param command The {@link CreateUserProfileCommand} Command
   * @return A {@link Result} containing the created {@link Profile} on success,
   *         or an {@link ApplicationError} on failure (validation or business rule violation)
   */
  Result<Profile, ApplicationError> handle(CreateUserProfileCommand command);

  /**
   * Handle Link Profile To User Command.
   *
   * <p>Binds a profile that already exists - typically one created through the profiles API
   * before its owner registered - to the platform account that now claims the same email.</p>
   *
   * @param command The {@link LinkProfileToUserCommand} Command
   * @return A {@link Result} containing the linked {@link Profile} on success,
   *         or an {@link ApplicationError} when the profile is missing or already owned
   */
  Result<Profile, ApplicationError> handle(LinkProfileToUserCommand command);
}
