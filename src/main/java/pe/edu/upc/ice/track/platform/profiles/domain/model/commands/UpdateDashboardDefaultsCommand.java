package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to replace the site and temperature range a dashboard opens on.
 *
 * <p>The raw values are turned into value objects - and validated - by the command service.</p>
 *
 * @param userId    identifier of the account owning the dashboard; required
 * @param siteId    identifier of the new default site; required
 * @param tempValue value of the new default temperature range; required
 * @param tempLabel label of the new default temperature range; required
 */
public record UpdateDashboardDefaultsCommand(Long userId, Long siteId, String tempValue, String tempLabel) {

  /**
   * Validates that the account identifier was supplied.
   */
  public UpdateDashboardDefaultsCommand {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
