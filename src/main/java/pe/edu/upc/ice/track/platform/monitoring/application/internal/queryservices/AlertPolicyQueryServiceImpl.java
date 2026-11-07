package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertPolicyQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;

import java.util.Optional;

/**
 * Default implementation of {@link AlertPolicyQueryService}, scoped by {@link EquipmentAccessPolicy}.
 */
@Service
public class AlertPolicyQueryServiceImpl implements AlertPolicyQueryService {

  private final AlertPolicyRepository alertPolicyRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;

  public AlertPolicyQueryServiceImpl(
      AlertPolicyRepository alertPolicyRepository, EquipmentAccessPolicy equipmentAccessPolicy) {
    this.alertPolicyRepository = alertPolicyRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<AlertPolicy> handle(GetAlertPolicyByEquipmentQuery query) {
    if (!equipmentAccessPolicy.canRead(query.equipmentId(), query.username(), query.callerRole())) {
      return Optional.empty();
    }
    return alertPolicyRepository.findApplicableTo(query.equipmentId());
  }
}
