package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import pe.edu.upc.ice.track.platform.shared.domain.model.valueobjects.UserId;

import java.util.Objects;

/**
 * LinkProfileToUserCommand record that represents the command to bind an existing profile to a
 * platform account.
 *
 * <p>Raised when a profile was created before its owner registered - through the profiles REST
 * API, for instance - and the account is now identified by the same email address. Linking keeps
 * a single profile per person instead of producing a second one on first sign-in.</p>
 *
 * @param profileId identifier of the profile to bind; required
 * @param userId    identifier of the account to bind it to; required
 */
public record LinkProfileToUserCommand(Long profileId, UserId userId) {

  /**
   * Validates that both sides of the link were supplied.
   */
  public LinkProfileToUserCommand {
    Objects.requireNonNull(profileId, "profileId must not be null");
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
