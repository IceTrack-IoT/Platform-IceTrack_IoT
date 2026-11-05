package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

import org.springframework.stereotype.Service;
import pe.edu.upc.ice.track.platform.monitoring.application.queryservices.AlertQueryService;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.queries.GetOpenAlertsByEquipmentQuery;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.time.LocalDateTime;

/**
 * ACL facade that exposes Monitoring and Alerting capabilities to other bounded contexts.
 *
 * <p>Every parameter and return type is a primitive or a {@link String} — no monitoring
 * aggregate, value object or repository is reachable from here. The primitives-only signature is
 * translated into calls against the monitoring application layer, and the results back into
 * primitives before returning. Consumed synchronously by Reporting &amp; Análisis; consumed
 * asynchronously, via {@code AlertRaisedIntegrationEvent}, by Notifications.</p>
 */
@Service
public class MonitoringContextFacade {

  private static final long NO_ALERT = 0L;

  private final AlertQueryService alertQueryService;
  private final AlertRepository alertRepository;

  public MonitoringContextFacade(AlertQueryService alertQueryService, AlertRepository alertRepository) {
    this.alertQueryService = alertQueryService;
    this.alertRepository = alertRepository;
  }

  /** @return the number of OPEN or ACKNOWLEDGED alerts currently active for the equipment. */
  public long countOpenAlertsByEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return 0L;
    }
    return alertQueryService.handle(new GetOpenAlertsByEquipmentQuery(equipmentId)).size();
  }

  /** @return the identifier of the most recent alert matching the status, or {@code 0L}. */
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

  /** @return how many alerts of the type were opened for the equipment within the range. */
  public long countAlertsByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
    return alertRepository.countByEquipmentIdAndTypeInRange(
        equipmentId, AlertType.valueOf(alertType.toUpperCase()), from, to);
  }

  /** @return total hours the equipment spent under alerts of the type within the range. */
  public double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to) {
    return alertRepository.totalDurationHoursByEquipmentIdAndTypeInRange(
        equipmentId, AlertType.valueOf(alertType.toUpperCase()), from, to);
  }
}
