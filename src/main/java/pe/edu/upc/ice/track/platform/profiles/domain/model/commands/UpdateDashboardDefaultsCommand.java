package pe.edu.upc.ice.track.platform.profiles.domain.model.commands;

import java.util.Objects;

/**
 * Command to replace the site and temperature range a dashboard opens on.
 *
 * <p>The raw values are turned into value objects - and validated - by the command service.</p>
 *
 * @param userId          identifier of the account owning the dashboard; required
 * @param siteId          identifier of the new default site; required
 * @param tempMin         lower bound of the new default temperature range; required
 * @param tempMax         upper bound of the new default temperature range; required
 * @param tempUnit        unit of the new default temperature range bounds, {@code "C"} or {@code "F"}; required
 * @param tempLabel       label of the new default temperature range; required
 * @param expectedVersion the version the caller edited, from {@code If-Match}; {@code null} skips the check
 */
public record UpdateDashboardDefaultsCommand(
    Long userId,
    Long siteId,
    Integer tempMin,
    Integer tempMax,
    String tempUnit,
    String tempLabel,
    Long expectedVersion) {

  /**
   * Validates that the account identifier was supplied.
   */
  public UpdateDashboardDefaultsCommand {
    Objects.requireNonNull(userId, "userId must not be null");
  }

  /**
   * Creates the command without a version precondition.
   */
  public UpdateDashboardDefaultsCommand(
      Long userId, Long siteId, Integer tempMin, Integer tempMax, String tempUnit, String tempLabel) {
    this(userId, siteId, tempMin, tempMax, tempUnit, tempLabel, null);
  }
}
