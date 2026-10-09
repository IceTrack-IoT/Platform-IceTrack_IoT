package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to create the dashboard configuration of a platform account.
 *
 * <p>The raw values are turned into value objects - and validated - by the command service.</p>
 *
 * @param userId    identifier of the account the configuration belongs to; required
 * @param siteId    identifier of the site the dashboard opens on; required
 * @param tempMin   lower bound of the temperature range the dashboard opens on; required
 * @param tempMax   upper bound of the temperature range the dashboard opens on; required
 * @param tempUnit  unit of the temperature range bounds, {@code "C"} or {@code "F"}; required
 * @param tempLabel label of the temperature range the dashboard opens on; required
 */
public record InitializeDashboardConfigCommand(
    Long userId,
    Long siteId,
    Integer tempMin,
    Integer tempMax,
    String tempUnit,
    String tempLabel) {

  /**
   * Validates that the account identifier was supplied.
   */
  public InitializeDashboardConfigCommand {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
