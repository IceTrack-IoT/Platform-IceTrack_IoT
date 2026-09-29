package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertPolicyQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertPolicyByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;

@Service
public class AlertPolicyQueryServiceImpl implements AlertPolicyQueryService {

  private final AlertPolicyRepository alertPolicyRepository;

  public AlertPolicyQueryServiceImpl(AlertPolicyRepository alertPolicyRepository) {
    this.alertPolicyRepository = alertPolicyRepository;
  }

  @Override
  public AlertPolicy handle(GetAlertPolicyByEquipmentQuery query) {
    return alertPolicyRepository.findByEquipmentId(query.equipmentId())
        .or(alertPolicyRepository::findDefaultPolicy)
        .orElseThrow(() -> new IllegalStateException(
            "No alert policy configured, not even a platform-wide default"));
  }
}
