package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to create the dashboard configuration of a platform account.
 *
 * <p>The raw values are turned into value objects - and validated - by the command service.</p>
 *
 * @param userId     identifier of the account the configuration belongs to; required
 * @param siteId     identifier of the site the dashboard opens on; required
 * @param tempValue  value of the temperature range the dashboard opens on; required
 * @param tempLabel  label of the temperature range the dashboard opens on; required
 */
public record InitializeDashboardConfigCommand(Long userId, Long siteId, String tempValue, String tempLabel) {

  /**
   * Validates that the account identifier was supplied.
   */
  public InitializeDashboardConfigCommand {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
