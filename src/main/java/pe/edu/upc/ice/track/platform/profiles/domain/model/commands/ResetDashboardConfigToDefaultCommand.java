package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to restore the default card layout of the dashboard of a platform account.
 *
 * @param userId identifier of the account owning the dashboard; required
 */
public record ResetDashboardConfigToDefaultCommand(Long userId) {

  /**
   * Validates that every component was supplied.
   */
  public ResetDashboardConfigToDefaultCommand {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
