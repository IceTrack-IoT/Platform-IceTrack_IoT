package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;

import java.util.Optional;

/** Application service handling read operations on {@link AlertPolicy}. */
public interface AlertPolicyQueryService {

  /**
   * @param query the query naming the equipment and the caller
   * @return the equipment-specific policy, falling back to the platform-wide default; empty when
   *         the caller may not read the equipment or no policy exists at all
   */
  Optional<AlertPolicy> handle(GetAlertPolicyByEquipmentQuery query);
}
