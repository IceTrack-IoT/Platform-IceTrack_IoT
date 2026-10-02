package pe.edu.upc.ice.track.platform.monitoring.application.internal.queryservices;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.Alert;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetAlertByIdQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.util.List;
import java.util.Optional;

@Service
public class AlertQueryServiceImpl implements AlertQueryService {

  private final AlertRepository alertRepository;

  public AlertQueryServiceImpl(AlertRepository alertRepository) {
    this.alertRepository = alertRepository;
  }

  @Override
  public Optional<Alert> handle(GetAlertByIdQuery query) {
    return alertRepository.findById(query.alertId());
  }

  @Override
  public List<Alert> handle(GetOpenAlertsByEquipmentQuery query) {
    return alertRepository.findByEquipmentIdAndStatusIn(
        query.equipmentId(), List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED));
  }
}
