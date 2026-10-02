package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;

/** Application service handling read operations on {@link AlertPolicy}. */
public interface AlertPolicyQueryService {

  /** @return the equipment-specific policy, falling back to the platform-wide default. */
  AlertPolicy handle(GetAlertPolicyByEquipmentQuery query);
}
