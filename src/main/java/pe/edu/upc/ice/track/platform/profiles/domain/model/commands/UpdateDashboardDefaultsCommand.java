package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to replace the site and temperature range a dashboard opens on.
 *
 * <p>The raw values are turned into value objects - and validated - by the command service.</p>
 *
 * @param userId    identifier of the account owning the dashboard; required
 * @param siteId    identifier of the new default site; required
 * @param tempMin   lower bound of the new default temperature range; required
 * @param tempMax   upper bound of the new default temperature range; required
 * @param tempUnit  unit of the new default temperature range bounds, {@code "C"} or {@code "F"}; required
 * @param tempLabel label of the new default temperature range; required
 */
public record UpdateDashboardDefaultsCommand(
    Long userId,
    Long siteId,
    Integer tempMin,
    Integer tempMax,
    String tempUnit,
    String tempLabel) {

  /**
   * Validates that the account identifier was supplied.
   */
  public UpdateDashboardDefaultsCommand {
    Objects.requireNonNull(userId, "userId must not be null");
  }
}
