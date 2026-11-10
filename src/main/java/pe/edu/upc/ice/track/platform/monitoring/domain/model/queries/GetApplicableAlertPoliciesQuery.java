package pe.edu.upc.ice.track.platform.monitoring.domain.model.queries;

import java.util.List;
import java.util.Objects;

/**
 * Query for the alert policy applicable to several units of the caller at once.
 *
 * <p>Naming no unit asks for every unit the caller owns. A unit that does not exist or belongs to
 * another owner is left out of the answer rather than refused, so the answer never tells whether an
 * identifier is somebody else's unit.</p>
 *
 * @param username     username of the authenticated caller; required
 * @param equipmentIds identifiers of the units asked for, at most {@link #MAX_EQUIPMENT_IDS}; empty
 *                     asks for every unit of the caller
 */
public record GetApplicableAlertPoliciesQuery(String username, List<Long> equipmentIds) {

  /** Largest number of units a single request may name. */
  public static final int MAX_EQUIPMENT_IDS = 100;

  /**
   * Normalizes the identifiers - no gap left by a trailing comma, no repetition - and validates
   * their number.
   *
   * @throws IllegalArgumentException when more than {@link #MAX_EQUIPMENT_IDS} units are named
   */
  public GetApplicableAlertPoliciesQuery {
    equipmentIds = equipmentIds == null
        ? List.of()
        : equipmentIds.stream().filter(Objects::nonNull).distinct().toList();
    if (equipmentIds.size() > MAX_EQUIPMENT_IDS) {
      throw new IllegalArgumentException(
          "Parameter 'equipment_id' must not name more than %d units".formatted(MAX_EQUIPMENT_IDS));
    }
  }
}
