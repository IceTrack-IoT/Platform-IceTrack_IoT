package pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.transform;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.rest.resources.AlertPolicyResource;

/** Maps an {@link AlertPolicy} into an {@link AlertPolicyResource}. */
public final class AlertPolicyResourceFromEntityAssembler {

  private AlertPolicyResourceFromEntityAssembler() {
  }

  public static AlertPolicyResource toResourceFromEntity(AlertPolicy policy) {
    return new AlertPolicyResource(
        policy.getId(), policy.getEquipmentId().equipmentId(), policy.getSustainedExcursionMinutes(),
        policy.getHysteresisMargin().celsius(), policy.getMissedSyncWindowsForOffline(),
        policy.isActive());
  }
}
