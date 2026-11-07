package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.aggregates.AlertPolicy;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertStatus;
import pe.edu.upc.ice.track.platform.monitoring.domain.model.valueobjects.AlertType;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertPolicyRepository;
import pe.edu.upc.ice.track.platform.monitoring.domain.repositories.AlertRepository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * ACL facade that exposes Monitoring and Alerting capabilities to other bounded contexts.
 *
 * <p>Every parameter and return type is a primitive or a {@link String} — no monitoring
 * aggregate, value object or repository is reachable from here. The primitives-only signature is
 * translated into calls against the monitoring application layer, and the results back into
 * primitives before returning. Consumed synchronously by Reporting &amp; Análisis; consumed
 * asynchronously, via {@code AlertRaisedIntegrationEvent}, by Notifications.</p>
 *
 * <p>Reads go straight to the repositories rather than through the query services: those are
 * scoped to an authenticated caller, while a cross-context caller asks about a unit it already
 * resolved on its own terms.</p>
 */
@Service
public class MonitoringContextFacade {

  private static final long NO_ALERT = 0L;

  private static final List<AlertStatus> ACTIVE_STATUSES = List.of(AlertStatus.OPEN, AlertStatus.ACKNOWLEDGED);

  private final AlertRepository alertRepository;
  private final AlertPolicyRepository alertPolicyRepository;

  public MonitoringContextFacade(AlertRepository alertRepository, AlertPolicyRepository alertPolicyRepository) {
    this.alertRepository = alertRepository;
    this.alertPolicyRepository = alertPolicyRepository;
  }

  /**
   * Returns the alert policy that applies to a unit: its own when it has one, the stored platform
   * default otherwise, and the built-in default when neither is stored.
   *
   * <p>Device Management includes it in the configuration the unit's edge gateway polls, which
   * evaluates excursions locally.</p>
   *
   * @param equipmentId identifier of the unit; {@code null} yields the platform default
   * @return the applicable settings, never {@code null}
   */
  @Transactional(readOnly = true)
  public AlertPolicySettings fetchApplicableAlertPolicy(Long equipmentId) {
    var policy = (equipmentId == null
        ? alertPolicyRepository.findDefaultPolicy()
        : alertPolicyRepository.findApplicableTo(equipmentId))
        .orElseGet(AlertPolicy::platformDefault);
    return new AlertPolicySettings(
        policy.getSustainedExcursionMinutes(),
        policy.getHysteresisMargin().celsius(),
        policy.getMissedSyncWindowsForOffline());
  }

  /** @return the number of OPEN or ACKNOWLEDGED alerts currently active for the equipment. */
  public long countOpenAlertsByEquipment(Long equipmentId) {
    if (equipmentId == null) {
      return 0L;
    }
    return alertRepository.findByEquipmentIdAndStatusIn(equipmentId, ACTIVE_STATUSES).size();
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
    return alertRepository.findByEquipmentIdAndStatusIn(equipmentId, ACTIVE_STATUSES).stream()
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
