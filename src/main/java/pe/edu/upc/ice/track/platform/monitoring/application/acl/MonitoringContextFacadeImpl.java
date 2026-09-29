package pe.edu.upc.ice.track.platform.monitoring.application.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.interfaces.acl.MonitoringContextFacade;

/**
 * Default implementation of {@link MonitoringContextFacade}.
 *
 * <p>Translates the agnostic, primitives-only signature of the facade into calls against the
 * monitoring application layer, and translates the result back into primitives before
 * returning — no monitoring domain type ever crosses this boundary.</p>
 */
@Service
public class MonitoringContextFacadeImpl implements MonitoringContextFacade {

  private static final long NO_ALERT = 0L;

  private final AlertQueryService alertQueryService;

  public MonitoringContextFacadeImpl(AlertQueryService alertQueryService) {
    this.alertQueryService = alertQueryService;
  }

  @Override
  public long countOpenAlertsByEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return 0L;
    }
    return alertQueryService.handle(new GetOpenAlertsByEquipmentQuery(equipmentId)).size();
  }

  @Override
  public Long fetchLatestAlertIdByEquipmentAndStatus(Long equipmentId, String status) {
    if (equipmentId == null || status == null) {
      return NO_ALERT;
    }
    AlertStatus parsedStatus;
    try {
      parsedStatus = AlertStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException ex) {
      return NO_ALERT;
    }
    return alertQueryService.handle(new GetOpenAlertsByEquipmentQuery(equipmentId)).stream()
        .filter(alert -> alert.getStatus() == parsedStatus)
        .findFirst()
        .map(alert -> alert.getId() == null ? NO_ALERT : alert.getId())
        .orElse(NO_ALERT);
  }
}
