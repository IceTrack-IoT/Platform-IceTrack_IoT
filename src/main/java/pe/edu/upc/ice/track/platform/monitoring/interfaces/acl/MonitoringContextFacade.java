package pe.edu.upc.ice.track.platform.monitoring.interfaces.acl;

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
}
