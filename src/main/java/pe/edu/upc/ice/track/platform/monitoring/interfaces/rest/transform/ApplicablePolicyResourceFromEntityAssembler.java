package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ApplicableAlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.ApplicablePolicyResource;

import java.util.List;

/** Maps an {@link ApplicableAlertPolicy} into an {@link ApplicablePolicyResource}. */
public final class ApplicablePolicyResourceFromEntityAssembler {

  /** Source reported for a unit that has a policy of its own. */
  private static final String SOURCE_EQUIPMENT = "EQUIPMENT";

  /** Source reported for a unit the platform-wide default applies to. */
  private static final String SOURCE_DEFAULT = "DEFAULT";

  private ApplicablePolicyResourceFromEntityAssembler() {
  }

  public static ApplicablePolicyResource toResourceFromEntity(ApplicableAlertPolicy applicablePolicy) {
    return new ApplicablePolicyResource(
        applicablePolicy.equipmentId(),
        applicablePolicy.isPlatformDefault() ? SOURCE_DEFAULT : SOURCE_EQUIPMENT,
        AlertPolicyResourceFromEntityAssembler.toResourceFromEntity(applicablePolicy.policy()));
  }

  /**
   * Converts a list of applicable policies, keeping its order.
   *
   * @param applicablePolicies the policies, already ordered
   * @return the resources, in the same order
   */
  public static List<ApplicablePolicyResource> toResourcesFromEntities(List<ApplicableAlertPolicy> applicablePolicies) {
    return applicablePolicies.stream()
        .map(ApplicablePolicyResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
  }
}
