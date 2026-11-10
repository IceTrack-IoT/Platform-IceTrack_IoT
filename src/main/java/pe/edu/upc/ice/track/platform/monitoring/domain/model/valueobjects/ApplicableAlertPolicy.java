package pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;

import java.util.Objects;

/**
 * The alert policy that applies to one unit: its own, or the platform-wide default when it has none.
 *
 * <p>Pairs the unit with the policy because the default policy is bound to no unit, so the policy
 * alone could not say which unit it was resolved for.</p>
 *
 * @param equipmentId identifier of the unit the policy applies to; required
 * @param policy      the applicable policy; required
 */
public record ApplicableAlertPolicy(Long equipmentId, AlertPolicy policy) {

  /**
   * Validates that both the unit and its policy are present.
   */
  public ApplicableAlertPolicy {
    Objects.requireNonNull(equipmentId, "equipmentId must not be null");
    Objects.requireNonNull(policy, "policy must not be null");
  }

  /**
   * Tells where the policy comes from.
   *
   * @return {@code true} when the unit has no policy of its own and the platform default applies
   */
  public boolean isPlatformDefault() {
    return policy.isPlatformDefault();
  }
}
