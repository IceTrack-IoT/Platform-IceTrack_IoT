package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import java.time.Instant;

/**
 * Query to list the maintenance windows of a unit, on behalf of its owner.
 *
 * @param equipmentId identifier of the unit; required
 * @param from        inclusive lower bound of the window start, or {@code null} for no lower bound
 * @param to          inclusive upper bound of the window start, or {@code null} for no upper bound
 * @param username    username of the authenticated caller; required
 */
public record GetMaintenanceWindowsByEquipmentQuery(Long equipmentId, Instant from, Instant to, String username) {

  /**
   * Validates the query.
   *
   * @throws IllegalArgumentException when the unit is missing or {@code from} is after {@code to}
   */
  public GetMaintenanceWindowsByEquipmentQuery {
    if (equipmentId == null || equipmentId <= 0) {
      throw new IllegalArgumentException("equipmentId must be a positive identifier");
    }
    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException("from must not be after to");
    }
  }
}
