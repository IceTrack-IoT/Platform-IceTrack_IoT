package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

import java.time.LocalDateTime;

/**
 * ACL facade that exposes Monitoring and Alerting capabilities to other bounded contexts.
 *
 * <p>Every parameter and return type is a primitive or a {@link String} — no monitoring
 * aggregate, value object or repository is reachable from here. Consumed synchronously by
 * Reporting &amp; Análisis; consumed asynchronously, via {@code AlertRaisedIntegrationEvent},
 * by Notifications.</p>
 */
public interface MonitoringContextFacade {

  /** @return the number of OPEN or ACKNOWLEDGED alerts currently active for the equipment. */
  long countOpenAlertsByEquipment(Long equipmentId);

  /** @return the identifier of the most recent alert matching the status, or {@code 0L}. */
  Long fetchLatestAlertIdByEquipmentAndStatus(Long equipmentId, String status);

  /** @return how many alerts of the type were opened for the equipment within the range. */
  long countAlertsByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to);

  /** @return total hours the equipment spent under alerts of the type within the range. */
  double fetchTotalAlertDurationHoursByEquipmentAndTypeInRange(
      Long equipmentId, String alertType, LocalDateTime from, LocalDateTime to);
}
