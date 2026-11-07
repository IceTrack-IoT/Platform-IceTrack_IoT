package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.application.internal.accesspolicies.EquipmentAccessPolicy;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link AlertQueryService}.
 *
 * <p>Every read is scoped by {@link EquipmentAccessPolicy}: an alert of a unit the caller may not
 * read is answered exactly like a missing one.</p>
 */
@Service
public class AlertQueryServiceImpl implements AlertQueryService {

  private final AlertRepository alertRepository;
  private final EquipmentAccessPolicy equipmentAccessPolicy;

  public AlertQueryServiceImpl(AlertRepository alertRepository, EquipmentAccessPolicy equipmentAccessPolicy) {
    this.alertRepository = alertRepository;
    this.equipmentAccessPolicy = equipmentAccessPolicy;
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Alert> handle(GetAlertByIdQuery query) {
    return alertRepository.findById(query.alertId())
        .filter(alert -> equipmentAccessPolicy.canRead(alert.getEquipmentId(), query.username(), query.callerRole()));
  }

  @Override
  @Transactional(readOnly = true)
  public List<Alert> handle(GetOpenAlertsByEquipmentQuery query) {
    if (!equipmentAccessPolicy.canRead(query.equipmentId(), query.username(), query.callerRole())) {
      return List.of();
    }
    return alertRepository.findByEquipmentIdAndStatusIn(
        query.equipmentId(), List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
  }
}
