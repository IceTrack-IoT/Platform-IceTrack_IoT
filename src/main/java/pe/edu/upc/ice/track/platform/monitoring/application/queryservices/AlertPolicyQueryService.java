package pe.edu.upc.ice.track.platform.monitoring.application.queryservices;

import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetApplicableAlertPoliciesQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.ApplicableAlertPolicy;

import java.util.List;
import java.util.Optional;

/** Application service handling read operations on {@link AlertPolicy}. */
public interface AlertPolicyQueryService {

  /**
   * @param query the query naming the equipment and the caller
   * @return the equipment-specific policy, falling back to the platform-wide default; empty when
   *         the caller may not read the equipment or no policy exists at all
   */
  Optional<AlertPolicy> handle(GetAlertPolicyByEquipmentQuery query);

  /**
   * Resolves the policy applicable to several units of the caller in one read.
   *
   * @param query the query naming the caller and the units, or no unit for all of the caller's
   * @return one entry per unit asked for that the caller owns, ordered by unit identifier; a unit
   *         that does not exist or belongs to another owner has no entry
   */
  List<ApplicableAlertPolicy> handle(GetApplicableAlertPoliciesQuery query);
}
