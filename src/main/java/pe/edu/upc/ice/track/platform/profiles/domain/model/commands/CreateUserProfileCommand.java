package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.profiles.domain.model.valueobjects.ProfileCreationData;

import java.util.Objects;

/**
 * CreateUserProfileCommand record that represents the command to create the profile of a newly
 * registered platform account.
 *
 * <p>Unlike {@link CreateProfileCommand}, which is driven by the profiles REST API and therefore
 * carries raw request values, this command is raised as the consequence of a registration that
 * happened in another bounded context. Its payload has already been translated into profiles
 * domain value objects by the listener, so the command service only has to resolve the matching
 * {@code UserProfileFactory} and persist the result.</p>
 *
 * @param profileCreationData the validated creation data; required
 */
public record CreateUserProfileCommand(ProfileCreationData profileCreationData) {

  /**
   * Validates that creation data was supplied.
   */
  public CreateUserProfileCommand {
    Objects.requireNonNull(profileCreationData, "profileCreationData must not be null");
  }
}
